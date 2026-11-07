# 階段 6：後端搬到 AWS 規劃書

> 狀態：規劃中（2026-11-07），尚未實作。操作步驟見 [AWS操作手冊.md](AWS操作手冊.md)。
> 價格與服務現況查詢日為 2026-11-07，實際以 AWS 官網為準。

## 1. 目標與範圍

- 後端（Spring Boot 4 / Java 21）從 Railway 搬到 AWS，功能與網址行為不變。
- **不搬**：前端留 GitHub Pages、資料庫留 MongoDB Atlas、寄信留 Resend、登入留 Google OAuth。
- **程式不改邏輯**：只新增部署用檔案（Dockerfile、GitHub Actions），換環境變數與前端的 API 網址。
- 搬家期間 Railway 繼續跑，確認 AWS 正常後才關，隨時可以切回。

## 2. 重要變化：App Runner 已不能用

系統規劃書 7.5 原本選 **AWS App Runner**，但 AWS 已在 **2026-04-30 停止接受新客戶**（既有客戶可繼續用，不再加新功能），官方建議改用 **Amazon ECS Express Mode**。本帳號沒用過 App Runner，所以需要改選平台。

## 3. 平台比較

| | **Lightsail 容器服務**（建議） | **ECS Express Mode** | Elastic Beanstalk（單機） |
|---|---|---|---|
| 是什麼 | AWS 的簡易版容器服務，固定月費 | ECS + Fargate + 負載平衡器（ALB）一鍵建好 | EC2 虛擬機 + 平台管理 |
| 月費估算 | 約 **US$10**（Micro：0.25 vCPU / 1 GB，含 500 GB 流量） | 約 **US$35 起**（Fargate 0.5 vCPU / 1 GB 約 $18 + ALB 約 $16 + 公用 IP 費） | 約 $12～15（t4g.small + 公用 IP） |
| HTTPS | 內建，給一個 `*.cs.amazonlightsail.com` 網址 | 內建，ALB 自帶 TLS | 單機沒有負載平衡器，要自己裝憑證（麻煩） |
| 設定難度 | 低，接近 Railway | 中（要建 IAM 角色、ECR） | 中高 |
| 擴充性 | 夠用（可調大或加台數） | 最好（自動擴展） | 普通 |
| 適合 | 個人 / 小型系統，用量穩定 | 之後要正式上線、多服務 | 不建議 |

**建議選 Lightsail 容器服務**：本系統只有一個後端、使用者少，Lightsail 最便宜、最接近 Railway 的操作方式，HTTPS 也不用自己處理。日後若用量變大，再照附錄改到 ECS Express Mode，程式與 Docker 映像檔都能沿用。

## 4. 目標架構

```
瀏覽器
  │  https://alexsuze.github.io/read-footprints/（GitHub Pages，不變）
  │
  ├──▶ Google 登入（不變）
  │
  └──▶ https://read-footprints-api.xxxx.<region>.cs.amazonlightsail.com
          AWS Lightsail 容器服務（Micro × 1）
          └─ Docker 映像檔：Java 21 + backend.jar
                │
                ├──▶ MongoDB Atlas（不變，read_footprints）
                └──▶ Resend（不變）
```

## 5. 決策事項（請使用者確認）

| # | 項目 | 建議 | 說明 |
|---|------|------|------|
| D1 | 平台 | Lightsail 容器服務 | 見第 3 節 |
| D2 | 區域 | **與 Atlas cluster 同區**（例如 Atlas 在 `ap-northeast-1` 東京就選東京） | 後端與資料庫同區延遲最低、流量不跨區。請到 Atlas 看 cluster 的區域 |
| D3 | 規格 | Micro（0.25 vCPU / 1 GB），1 台 | Spring Boot 需要約 1 GB 記憶體，Nano（0.5 GB）太小。若啟動或回應明顯變慢再升 Small |
| D4 | 網址 | 先用 Lightsail 給的預設網址 | API 網址只有前端在用，不需要自訂網域；要自訂網域可之後再加（需 Lightsail 憑證） |
| D5 | 部署方式 | GitHub Actions：push 到 `main` 且 `backend/` 有變動就自動建映像檔並部署 | 跟現在 Railway 一樣自動。AWS 金鑰用 GitHub OIDC，不存長期金鑰 |
| D6 | 環境變數 | 存在 Lightsail 部署設定（同 Railway） | Lightsail 沒有接 Secrets Manager；改用 ECS 時再搬到 Secrets Manager |
| D7 | Atlas 網路白名單 | 維持現況（Railway 時應是 `0.0.0.0/0`），靠帳密 + TLS 保護 | Lightsail 對外 IP 不固定，無法只開單一 IP。要鎖 IP 得用 Atlas 付費方案的 Private Endpoint 或固定 IP 代理，不划算 |
| D8 | 預算警示 | AWS Budgets 設每月 US$15，用範本預設的警示（快超過時寄信） | 避免誤開大規格或忘了關東西 |
| D9 | Railway 何時關 | AWS 穩定運作 1～2 週後 | 這段期間兩邊都連同一個 Atlas，資料一致；前端只會打其中一邊 |

## 6. 需要的程式 / 檔案變更（實作時再做）

| 檔案 | 內容 |
|------|------|
| `backend/Dockerfile` | 兩段式建置：`eclipse-temurin:21-jdk` 用 `./mvnw package` 打包 → `eclipse-temurin:21-jre` 只放 jar。以非 root 使用者執行，`EXPOSE 8080`，JVM 參數 `-XX:MaxRAMPercentage=75` 讓記憶體跟著容器大小走 |
| `backend/.dockerignore` | 排除 `target/`、`.env` 等 |
| `.github/workflows/deploy-backend.yml` | push 到 `main` 且 `backend/**` 有變動時：跑測試 → 建映像檔 → 推到 Lightsail → 建新部署 → 等健康檢查通過 |
| `README.md`、系統規劃書 7.4～7.6 | 部署章節改成 AWS，Railway 改列為「已停用」 |
| `backend/railway.toml`、`nixpacks.toml` | Railway 關掉後刪除 |

不需要改的：
- **CORS**：前端網址不變，`CORS_ALLOWED_ORIGIN` 照舊。
- **Google OAuth**：登入是在前端網域進行，後端網址換了不影響。
- **資料庫**：同一個 Atlas，不用搬資料。
- **健康檢查**：沿用 `/actuator/health/liveness`。

## 7. 切換步驟（概要）

1. 建 AWS 帳號安全設定（MFA、管理用帳號、預算警示）。
2. 實作 Dockerfile，本機建映像檔並跑起來確認。
3. 建 Lightsail 容器服務，手動推第一版映像檔、填環境變數、部署。
4. 用 Lightsail 網址測 `/api/ping`、`/actuator/health`，再用 curl 帶通行證測幾支 API。
5. GitHub Secret `VITE_API_BASE_URL` 改成 Lightsail 網址，重跑前端部署。
6. 照驗證清單走一遍（登入、書架、活動、統計、借書、寄信）。
7. 加上 GitHub Actions 自動部署，push 一次確認會自動更新。
8. 觀察 1～2 週（記錄、費用、速度），沒問題就關 Railway、刪 Railway 設定檔。

詳細操作見 [AWS操作手冊.md](AWS操作手冊.md)。

## 8. 回復方案

- 任何時候有問題：把 GitHub Secret `VITE_API_BASE_URL` 改回 Railway 網址，重跑前端部署（約 2 分鐘），就回到原狀。
- Railway 在第 8 步之前都不關，資料都在 Atlas，兩邊共用，不會遺失。

## 9. 風險與注意事項

| 風險 | 影響 | 對策 |
|------|------|------|
| 0.25 vCPU 啟動慢 | Spring Boot 啟動可能要 1～2 分鐘，部署時健康檢查失敗 | 健康檢查寬限時間調長；真的太慢就升 Small |
| 記憶體不足 | 1 GB 下 JVM 被系統砍掉（OOM） | `MaxRAMPercentage=75`；觀察記錄，必要時升規格 |
| 切換期間通行證 | `JWT_SECRET` 兩邊要一樣，否則使用者切過去要重新登入 | AWS 填跟 Railway 相同的 `JWT_SECRET` |
| 環境變數放在部署設定 | 能進 AWS 主控台的人看得到 | AWS 帳號開 MFA、不共用帳號 |
| Atlas 白名單全開 | 資料庫暴露在網路上 | 資料庫帳號用長密碼、只給 `read_footprints` 讀寫權限 |
| 費用 | 開錯規格或忘了關 | AWS Budgets 警示（D8） |
| 管理員同時連兩邊後端 | 切換期間不同分頁可能打到不同後端 | 都寫同一個 Atlas，資料一致；切換後重新整理即可 |

## 10. 費用估算（Lightsail 方案，每月）

| 項目 | 費用 |
|------|------|
| Lightsail 容器服務 Micro × 1 | 約 US$10（含 500 GB 流量，本系統用不到） |
| MongoDB Atlas | 不變（目前免費層） |
| GitHub Pages / Actions | 免費 |
| Resend | 不變 |
| **合計** | **約 US$10** |

## 11. 驗證清單（實作時使用）

- [ ] `https://<Lightsail 網址>/api/ping` 回 `ok: true`，`/actuator/health/liveness` 回 `UP`。
- [ ] 前端改打 AWS 後，Google 登入、總覽、圖書館、活動、統計、設定都正常。
- [ ] 新增 / 修改書籍、書封上傳、閱讀狀態切換正常。
- [ ] 借書申請與歸還，管理員有收到兩封信。
- [ ] push 一次 `backend/` 的修改，GitHub Actions 自動部署成功。
- [ ] 觀察 1 週，Lightsail 記錄沒有錯誤、AWS 帳單符合預估。
- [ ] 關閉 Railway 後網站仍正常。

## 附錄 A：改用 ECS Express Mode 的差異

若之後要改用 ECS Express Mode（或一開始就想用）：

- 映像檔推到 **Amazon ECR**（容器映像倉庫），不是推到 Lightsail。
- 要先建兩個 IAM 角色：**任務執行角色**（拉映像檔、讀密鑰，掛 `AmazonECSTaskExecutionRolePolicy`）與**基礎設施角色**（讓 ECS 建負載平衡器、安全群組等）。
- 健康檢查路徑預設是 `/ping`，要改成 `/actuator/health/liveness`。
- 環境變數可以改用 Secrets Manager / Parameter Store 存密鑰，比較安全。
- 費用約每月 US$35 以上（Fargate + ALB + 公用 IP）。
- 操作步驟見操作手冊附錄。

## 參考資料

- AWS App Runner 停止接受新客戶：[AWS App Runner availability change](https://docs.aws.amazon.com/ja_jp/apprunner/latest/dg/apprunner-availability-change.html)
- ECS Express Mode：[官方說明](https://docs.aws.amazon.com/zh_tw/AmazonECS/latest/developerguide/express-service-overview.html)、[CLI create-express-gateway-service](https://docs.aws.amazon.com/cli/latest/reference/ecs/create-express-gateway-service.html)、[ECS 價格](https://aws.amazon.com/ecs/pricing/)
- Lightsail 容器服務：[FAQ](https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-faq-containers.html)、[部署設定](https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-container-services-deployments.html)、[CLI create-container-service-deployment](https://docs.aws.amazon.com/cli/latest/reference/lightsail/create-container-service-deployment.html)、[價格](https://aws.amazon.com/lightsail/pricing/)
- MongoDB Atlas 白名單：[IP Access List](https://www.mongodb.com/docs/atlas/security/ip-access-list/)
