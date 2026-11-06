# 讀跡 read-footprints

個人閱讀足跡管理系統 2.0：擬真書架、閱讀活動、閱讀統計、向管理員借書。Google 登入、管理員核准制。
規劃書見 [Doc/系統規劃書.md](Doc/系統規劃書.md)，開發進度見 [Doc/開發進度.md](Doc/開發進度.md)。
前身為 [book-review](https://github.com/alexsuze/book-review) 1.x。

## Repo 結構

```
read-footprints/
├── Doc/                 # 規劃書、開發進度
├── frontend/            # Vue 3 + Vite + Pinia + Bootstrap 5
├── backend/             # Java 21 + Spring Boot 4（Maven Wrapper）
└── .github/workflows/   # 前端 GitHub Pages 自動部署
```

## 本機開發

需要 Node.js 20+、Java 21+、MongoDB 連線字串（Atlas 免費層或本機）。

### 後端
```bash
cd backend
# 環境變數見 backend/.env.example，至少需要 MONGODB_URI；要登入還需 GOOGLE_CLIENT_ID、ADMIN_EMAIL
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```
- `GET http://localhost:8080/actuator/health/liveness` 應回 `{"status":"UP"}`
- `GET http://localhost:8080/api/ping` 不需登入；其他 `/api/**` 需 `Authorization: Bearer <通行證>`

### 前端
```bash
cd frontend
cp .env.example .env.local      # 填 API 網址與 Google 用戶端 ID
npm install
npm run dev                     # http://localhost:5173
```
Google 用戶端 ID 的「已授權的 JavaScript 來源」需包含 `http://localhost:5173`。

## 部署（第一階段：GitHub Pages + Railway + Atlas）

### 前端 → GitHub Pages
1. repo Settings → Pages → Source 選 **GitHub Actions**。
2. Settings → Secrets and variables → Actions 新增 `VITE_API_BASE_URL`（Railway 後端網址，須含 `https://`，結尾不加斜線）、`VITE_GOOGLE_CLIENT_ID`。
3. push 到 `main` 且 `frontend/` 有變動即自動部署到 `https://alexsuze.github.io/read-footprints/`。

### 後端 → Railway
1. 新建專案 → Deploy from GitHub repo → 選此 repo，服務 Settings → **Root Directory** 設 `backend`。
2. Variables：

   | 變數 | 說明 |
   |------|------|
   | `MONGODB_URI` | Atlas 連線字串 |
   | `MONGODB_DATABASE` | `read_footprints` |
   | `GOOGLE_CLIENT_ID` | Google OAuth 用戶端 ID（可與 to-do-schedule 共用，同一個 github.io 來源） |
   | `ADMIN_EMAIL` | 管理員的 Google Email，第一次登入自動成為管理員 |
   | `JWT_SECRET` | 至少 32 字元，可填 `${{ secret(64) }}` 自動產生 |
   | `CORS_ALLOWED_ORIGIN` | `https://alexsuze.github.io` |
   | `FRONTEND_URL` | `https://alexsuze.github.io/read-footprints/` |
   | `RESEND_API_KEY` | Resend API Key |
   | `RESEND_FROM_EMAIL` | `onboarding@resend.dev` |
   | `LEGACY_MONGODB_URI` | 匯入 1.0 資料時才需要，1.0 的 Atlas 連線字串，資料庫固定讀 `book_admin` |

3. push 到 `main` 即自動部署，建置與啟動指令在 `backend/railway.toml`。

### MongoDB Atlas
同一 cluster 新建資料庫 `read_footprints`；1.0 的 `book_admin` 不動，階段 1 匯入時另提供其連線字串。
