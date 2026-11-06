import axios from 'axios'
import { authToken, clearSession } from '../services/session'
import { toast } from '../components/AppToast.vue'

// 所有後端請求皆經由此 instance，自動帶入登入通行證
const http = axios.create({
  baseURL: `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api`,
  timeout: 15000
})

http.interceptors.request.use(config => {
  const token = authToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 統一顯示錯誤：後端中文訊息在 Problem Details 的 detail
// 401（通行證過期 / 帳號被停用）→ 清除登入狀態回登入頁；登入本身的錯誤交給登入頁顯示
http.interceptors.response.use(null, (e) => {
  const isLogin = e.config?.url === '/auth/google'
  if (e.response?.status === 401 && !isLogin) {
    clearSession()
    toast(e.response.data?.detail || '請重新登入', 'warning')
    window.location.hash = '#/login'
  } else if (!isLogin && !e.config?.silent) { // silent：背景讀取失敗不打擾
    toast(e.response?.data?.detail || e.message, 'danger')
  }
  return Promise.reject(e)
})

export default http
