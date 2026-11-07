# AWS 操作手冊（階段 6：後端搬到 Lightsail 容器服務）

> 對應 [階段6_AWS規劃書.md](階段6_AWS規劃書.md)。撰寫日 2026-11-07。
> AWS 主控台的按鈕名稱偶爾會改，找不到時以畫面上相近的字為準；CLI 指令以 AWS CLI v2 為準。
> 標 **【實作後】** 的步驟要等 Dockerfile 與 GitHub Actions 做好才能執行。

## 目錄

1. [事前準備](#1-事前準備)
2. [AWS 帳號安全設定](#2-aws-帳號安全設定)
3. [確認 MongoDB Atlas](#3-確認-mongodb-atlas)
4. [建立 Lightsail 容器服務](#4-建立-lightsail-容器服務)
5. [第一次部署](#5-第一次部署)
6. [驗證後端](#6-驗證後端)
7. [前端切換到 AWS](#7-前端切換到-aws)
8. [設定 GitHub Actions 自動部署](#8-設定-github-actions-自動部署)
9. [日常操作](#9-日常操作)
10. [關閉 Railway](#10-關閉-railway)
11. [疑難排解](#11-疑難排解)
12. [附錄：改用 ECS Express Mode](#12-附錄改用-ecs-express-mode)

---

## 1. 事前準備

| 需要 | 說明 |
|------|------|
| AWS 帳號 | 沒有就到 aws.amazon.com 註冊，需要信用卡 |
| Railway 的環境變數 | 到 Railway 服務的 Variables 頁把值抄下來（第 5 節要用），**`JWT_SECRET` 一定要抄**，兩邊一樣使用者才不用重新登入 |
| GitHub repo 管理權限 | 改 Secrets、設定 Actions |
| （選用）本機工具 | 只有走「方法 B 手動部署」才需要：AWS CLI v2、lightsailctl 外掛、Docker |

## 2. AWS 帳號安全設定

只做一次。

### 2.1 根帳號開 MFA
1. 用註冊的 Email 登入 AWS 主控台（這是「根帳號」，權限最大）。
2. 右上角帳號名稱 → **Security credentials（安全憑證）**。
3. **Multi-factor authentication (MFA)** → **Assign MFA device**，用手機驗證 App（Google Authenticator 等）掃碼綁定。

### 2.2 建一個平常用的管理帳號（不要每天用根帳號）
1. 主控台上方搜尋 **IAM Identity Center** → **Enable**。
2. **Users** → **Add user**，填自己的 Email，建立後到信箱設定密碼並綁 MFA。
3. **Permission sets** → **Create permission set** → 選 **Predefined** 的 `AdministratorAccess`。
4. **AWS accounts** → 勾選自己的帳號 → **Assign users or groups** → 選剛建的使用者與 `AdministratorAccess`。
5. 之後用 Identity Center 給的登入網址（AWS access portal URL）登入，存成書籤。

### 2.3 預算警示
1. 搜尋 **Billing and Cost Management** → **Budgets** → **Create budget**。
2. 選 **Use a template** → **Monthly cost budget**，金額填 **15**（USD），Email 填自己的。
3. 建立後，超過 85% 與預估超過 100% 都會寄信（範本預設）。

### 2.4 選區域
主控台右上角的區域選單選定一個，之後所有東西都建在這區。**建議跟 Atlas cluster 同區**（第 3 節查）。例如 Atlas 在 `AWS / Tokyo (ap-northeast-1)` 就選「亞太區域（東京）」。

## 3. 確認 MongoDB Atlas

1. 登入 Atlas → 左側 **Database** → 看 cluster 卡片上的區域（例如 `AWS Tokyo (ap-northeast-1)`），記下來給 2.4 用。
2. 左側 **Network Access**：
   - 目前 Railway 連得上，通常是有一筆 `0.0.0.0/0`（允許所有 IP）。**保留不動**，Lightsail 的對外 IP 不固定，沒辦法只開一個 IP。
   - 若不是 `0.0.0.0/0` 而是特定 IP，就要新增 `0.0.0.0/0`，否則 Lightsail 連不上。
3. 左側 **Database Access**：確認後端用的帳號密碼夠長（建議 20 字以上隨機），權限只有 `read_footprints` 的 readWrite 最好。

## 4. 建立 Lightsail 容器服務

1. 主控台搜尋 **Lightsail**，進入後確認右上角區域正確。
2. 上方分頁 **Containers** → **Create container service**。
3. **Container service location**：確認是 2.4 選的區域。
4. **Choose the capacity**：
   - **Power** 選 **Micro**（0.25 vCPU、1 GB 記憶體）。
   - **Scale** 填 **1**。
5. **Set up your first deployment**：先**不要**設定（第 5 節再部署）。
6. **Identify your service**：名稱填 `read-footprints-api`。
7. 按 **Create container service**，等狀態從 Pending 變 **Ready**（幾分鐘）。
8. 服務頁上方會顯示 **Public domain**，長得像：
   ```
   https://read-footprints-api.xxxxxxxxxx.ap-northeast-1.cs.amazonlightsail.com/
   ```
   這就是之後的後端網址，記下來（第 7 節要用）。

> 注意：容器服務建好就開始計費（停用也照算），不用時要「刪除」才不會收錢。

## 5. 第一次部署

兩種方法擇一。**方法 A 不用在自己電腦裝任何東西，建議用這個。**

### 5.1 要填的環境變數

| 變數 | 值 |
|------|----|
| `MONGODB_URI` | 跟 Railway 相同的 Atlas 連線字串 |
| `MONGODB_DATABASE` | `read_footprints` |
| `GOOGLE_CLIENT_ID` | 跟 Railway 相同 |
| `ADMIN_EMAIL` | 跟 Railway 相同 |
| `JWT_SECRET` | **跟 Railway 相同**（不同的話所有人要重新登入） |
| `CORS_ALLOWED_ORIGIN` | `https://alexsuze.github.io` |
| `FRONTEND_URL` | `https://alexsuze.github.io/read-footprints/` |
| `RESEND_API_KEY` | 跟 Railway 相同 |
| `RESEND_FROM_EMAIL` | 跟 Railway 相同（目前 `onboarding@resend.dev`） |

不需要：`PORT`（固定 8080）、`LEGACY_MONGODB_URI`（已停用）。

### 5.2 健康檢查設定

| 欄位 | 值 |
|------|----|
| Path | `/actuator/health/liveness` |
| Success codes | `200` |
| Interval | 30 秒 |
| Timeout | 10 秒 |
| Healthy threshold | 2 |
| Unhealthy threshold | 5 |

0.25 vCPU 啟動 Spring Boot 可能要 1～2 分鐘，Unhealthy threshold 設 5 是為了給足時間。

### 方法 A：用 GitHub Actions 推第一版 【實作後】

1. 先完成第 8 節（GitHub 連 AWS 的權限）。
2. GitHub repo → **Actions** → 左側 **Deploy backend to AWS** → **Run workflow**。
3. 第一次執行時工作流程只會推映像檔，並在 Lightsail 建一個沒有環境變數的部署。接著：
4. Lightsail → 服務 `read-footprints-api` → **Deployments** → **Modify your deployment**：
   - 容器名稱 `api`，映像檔選剛推上來的 `:read-footprints-api.api.N`（最新一個）。
   - **Open ports**：`8080`，協定 `HTTP`。
   - **Environment variables**：照 5.1 逐筆新增。
   - **Public endpoint**：選容器 `api`，填 5.2 的健康檢查。
   - 按 **Save and deploy**。
5. 之後每次自動部署，工作流程會沿用 Lightsail 上目前的環境變數與健康檢查，只換映像檔，所以**環境變數只要在 Lightsail 主控台維護**。

### 方法 B：從自己電腦手動部署 【實作後】

1. 安裝 [AWS CLI v2](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)、[lightsailctl 外掛](https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-install-software.html)、Docker Desktop。
2. 登入（用 2.2 的 Identity Center 帳號）：
   ```bash
   aws configure sso
   ```
3. 在 repo 的 `backend/` 建映像檔：
   ```bash
   docker build -t read-footprints-api:latest .
   ```
4. 推到 Lightsail（區域換成自己的）：
   ```bash
   aws lightsail push-container-image --region ap-northeast-1 --service-name read-footprints-api --label api --image read-footprints-api:latest
   ```
   最後會印出映像檔名稱，例如 `:read-footprints-api.api.1`。
5. 照方法 A 第 4 步在主控台建部署；或用 CLI（先把 `containers.json`、`endpoint.json` 存在電腦上，**不要 commit，裡面有密碼**）：

   `containers.json`
   ```json
   {
     "api": {
       "image": ":read-footprints-api.api.1",
       "ports": { "8080": "HTTP" },
       "environment": {
         "MONGODB_URI": "mongodb+srv://...",
         "MONGODB_DATABASE": "read_footprints",
         "GOOGLE_CLIENT_ID": "...",
         "ADMIN_EMAIL": "...",
         "JWT_SECRET": "...",
         "CORS_ALLOWED_ORIGIN": "https://alexsuze.github.io",
         "FRONTEND_URL": "https://alexsuze.github.io/read-footprints/",
         "RESEND_API_KEY": "...",
         "RESEND_FROM_EMAIL": "onboarding@resend.dev"
       }
     }
   }
   ```

   `endpoint.json`
   ```json
   {
     "containerName": "api",
     "containerPort": 8080,
     "healthCheck": {
       "path": "/actuator/health/liveness",
       "successCodes": "200",
       "intervalSeconds": 30,
       "timeoutSeconds": 10,
       "healthyThreshold": 2,
       "unhealthyThreshold": 5
     }
   }
   ```

   ```bash
   aws lightsail create-container-service-deployment --region ap-northeast-1 --service-name read-footprints-api --containers file://containers.json --public-endpoint file://endpoint.json
   ```

### 部署完成的樣子
Lightsail 服務頁 **Deployments** 顯示 **Active**，狀態是 **Running**。如果一直 Deploying 後變 Failed，看第 11 節。

## 6. 驗證後端

把 `<API>` 換成第 4 節的 Public domain（結尾不要斜線）：

```bash
curl -s <API>/api/ping
```
應回 `{"time":"...","ok":true}`。

```bash
curl -s <API>/actuator/health/liveness
```
應回 `{"status":"UP"}`。

```bash
curl -s -o /dev/null -w '%{http_code}\n' <API>/api/books
```
沒帶通行證應回 `401`，代表登入保護正常。

## 7. 前端切換到 AWS

1. GitHub repo → **Settings** → **Secrets and variables** → **Actions**。
2. 編輯 `VITE_API_BASE_URL`，改成 `<API>`（含 `https://`，結尾不要斜線）。
3. **Actions** → **Deploy frontend to GitHub Pages** → **Run workflow**（分支選 `main`）。
4. 約 1～2 分鐘後，開網站重新整理，照規劃書第 11 節的驗證清單測一遍。
5. 瀏覽器開發者工具 → Network，確認 API 請求打的是 `cs.amazonlightsail.com`。

**要切回 Railway**：把 `VITE_API_BASE_URL` 改回 Railway 網址，重跑一次前端部署即可。

## 8. 設定 GitHub Actions 自動部署

讓 GitHub 用「OIDC」向 AWS 換臨時權限，不用把 AWS 金鑰存在 GitHub。

### 8.1 建立 GitHub 身分提供者（只做一次）
1. 主控台搜尋 **IAM** → 左側 **Identity providers** → **Add provider**。
2. **Provider type**：OpenID Connect。
3. **Provider URL**：`https://token.actions.githubusercontent.com`
4. **Audience**：`sts.amazonaws.com`
5. **Add provider**。

### 8.2 建立給 GitHub 用的角色
1. IAM → **Roles** → **Create role**。
2. **Trusted entity type**：Web identity。
3. **Identity provider**：`token.actions.githubusercontent.com`；**Audience**：`sts.amazonaws.com`。
4. **GitHub organization**：`alexsuze`；**GitHub repository**：`read-footprints`；**GitHub branch**：`main`。
5. 下一步權限先跳過（8.3 再加），角色名稱填 `github-deploy-read-footprints` → 建立。
6. 打開這個角色，確認 **Trust relationships** 裡有：
   ```json
   "StringEquals": { "token.actions.githubusercontent.com:aud": "sts.amazonaws.com" },
   "StringLike": { "token.actions.githubusercontent.com:sub": "repo:alexsuze/read-footprints:ref:refs/heads/main" }
   ```
   這樣只有這個 repo 的 `main` 分支能用這個角色。

### 8.3 給角色最小權限
角色頁 → **Add permissions** → **Create inline policy** → **JSON**，貼上（`<區域>`、`<帳號ID>` 換成自己的，帳號 ID 在右上角帳號選單）：

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "lightsail:GetContainerServices",
        "lightsail:GetContainerServiceDeployments",
        "lightsail:GetContainerImages",
        "lightsail:CreateContainerServiceRegistryLogin",
        "lightsail:RegisterContainerImage",
        "lightsail:CreateContainerServiceDeployment",
        "lightsail:DeleteContainerImage"
      ],
      "Resource": "*"
    }
  ]
}
```

名稱填 `lightsail-deploy`。（Lightsail 這幾個動作多數不支援限定單一資源，所以 `Resource` 用 `*`；權限只有 Lightsail 容器相關，碰不到其他服務。）

### 8.4 把角色交給 GitHub
1. 複製角色的 **ARN**（像 `arn:aws:iam::123456789012:role/github-deploy-read-footprints`）。
2. GitHub repo → **Settings** → **Secrets and variables** → **Actions** → **Variables** 分頁 → 新增：
   - `AWS_ROLE_ARN` = 上面的 ARN
   - `AWS_REGION` = 例如 `ap-northeast-1`
   - `LIGHTSAIL_SERVICE` = `read-footprints-api`
3. 這三個不是密碼，放 Variables 即可。

### 8.5 測試 【實作後】
push 一個 `backend/` 的小修改到 `main`，或在 Actions 手動 **Run workflow**，看 **Deploy backend to AWS** 是否綠勾，Lightsail 的 Deployments 是否多一版。

## 9. 日常操作

| 要做的事 | 怎麼做 |
|----------|--------|
| 看後端記錄 | Lightsail → 服務 → **Logs** 分頁；或 `aws lightsail get-container-log --service-name read-footprints-api --container-name api` |
| 改環境變數 | Lightsail → **Deployments** → **Modify your deployment** → 改值 → **Save and deploy**（會重新部署一版，約 2～3 分鐘） |
| 重新部署同一版 | 同上，不改任何值直接 **Save and deploy** |
| 回到上一版 | **Deployments** → **Deployment versions** 找上一個 Active 過的版本 → **Modify and redeploy** |
| 升規格（變慢或記憶體不足） | 服務頁 → **Capacity** → **Change capacity** → 選 Small，不用重新部署 |
| 看費用 | Billing and Cost Management → **Bills**，Lightsail 會單獨一行 |
| 換 `JWT_SECRET` | 改環境變數後所有人都要重新登入（舊通行證失效），只有懷疑外洩時才換 |
| 映像檔太多 | Lightsail → **Images** 分頁刪掉不用的舊版本（自動部署工作流程會順便清掉較舊的版本，只留最近幾版） |

## 10. 關閉 Railway

觀察 1～2 週都正常後：
1. 確認網站打的是 AWS（第 7 節第 5 步）。
2. Railway → 專案 → 服務 → **Settings** → 最下面 **Delete service**（或先 **Remove** 部署觀察幾天）。
3. 告訴我，我會刪掉 `backend/railway.toml`、`nixpacks.toml`，並更新 README 與規劃書的部署章節。

## 11. 疑難排解

| 狀況 | 可能原因 | 處理 |
|------|----------|------|
| 部署一直 Deploying 後 Failed | 健康檢查在 Spring Boot 啟動完之前就判定失敗 | 看 Logs 有沒有 `Started BackendApplication`；把 Unhealthy threshold 調大或升 Small |
| Logs 出現 `OutOfMemoryError` 或容器一直重啟 | 1 GB 不夠 | 升 Small；或在 Dockerfile 調低 `MaxRAMPercentage` |
| Logs 出現 `MongoTimeoutException` / 連不到資料庫 | Atlas Network Access 沒開 `0.0.0.0/0`、或 `MONGODB_URI` 填錯 | 照第 3 節檢查 |
| Logs 出現 `JWT_SECRET 至少需要 32 字元` | 環境變數沒填或太短 | 補上跟 Railway 相同的值 |
| 前端顯示網路錯誤、瀏覽器 Console 有 CORS 錯誤 | `CORS_ALLOWED_ORIGIN` 不對 | 必須是 `https://alexsuze.github.io`（沒有路徑、沒有結尾斜線） |
| 前端還是打 Railway | 前端沒有重新部署，或瀏覽器快取 | 重跑前端部署、強制重新整理 |
| 使用者都被登出 | `JWT_SECRET` 跟 Railway 不同 | 改成相同值；或請大家重新登入即可 |
| 收不到借書通知信 | `RESEND_API_KEY` 或 `ADMIN_EMAIL` 沒填 | Logs 搜尋「借書通知寄送失敗」 |
| GitHub Actions 出現 `Not authorized to perform sts:AssumeRoleWithWebIdentity` | 角色信任設定的 repo / 分支不符 | 檢查 8.2 第 6 步 |

## 12. 附錄：改用 ECS Express Mode

用量變大、需要自動擴展時再考慮。大致流程：

1. **ECR**：主控台搜尋 ECR → **Create repository**，名稱 `read-footprints-api`，把映像檔推上去（`docker push`）。
2. **IAM 角色**：
   - 任務執行角色：信任 `ecs-tasks.amazonaws.com`，掛 `AmazonECSTaskExecutionRolePolicy`；要用 Secrets Manager 的話再加讀取該密鑰的權限。
   - 基礎設施角色：讓 ECS 建負載平衡器、目標群組、安全群組（主控台建立 Express 服務時可以一鍵建立）。
3. **密鑰**：把 `MONGODB_URI`、`JWT_SECRET`、`RESEND_API_KEY` 存到 **Secrets Manager**，在服務設定裡引用，不要直接寫在環境變數。
4. **建立 Express 服務**：ECS → **Express mode** → 選 ECR 映像檔、兩個角色、容器埠 `8080`，健康檢查路徑改成 `/actuator/health/liveness`（預設是 `/ping`），CPU 0.5 vCPU / 記憶體 1 GB。
5. 建好後會給一個 HTTPS 網址，照第 6、7 節驗證與切換前端。
6. 費用約每月 US$35 以上，記得調整 2.3 的預算金額。

## 參考資料

- [AWS App Runner availability change](https://docs.aws.amazon.com/ja_jp/apprunner/latest/dg/apprunner-availability-change.html)
- [Lightsail 容器服務 FAQ](https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-faq-containers.html)
- [Lightsail 容器部署](https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-container-services-deployments.html)
- [CLI：create-container-service-deployment](https://docs.aws.amazon.com/cli/latest/reference/lightsail/create-container-service-deployment.html)
- [Lightsail 價格](https://aws.amazon.com/lightsail/pricing/)
- [ECS Express Mode](https://docs.aws.amazon.com/zh_tw/AmazonECS/latest/developerguide/express-service-overview.html)
- [CLI：create-express-gateway-service](https://docs.aws.amazon.com/cli/latest/reference/ecs/create-express-gateway-service.html)
- [ECS 價格](https://aws.amazon.com/ecs/pricing/)
- [MongoDB Atlas IP Access List](https://www.mongodb.com/docs/atlas/security/ip-access-list/)
