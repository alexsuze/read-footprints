import { createRouter, createWebHashHistory } from 'vue-router'
import { authToken } from '../services/session'

// hash 模式，避免 GitHub Pages 子路徑重新整理 404
const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginPage.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'overview', component: () => import('../views/OverviewPage.vue') },
      { path: 'library', name: 'library', component: () => import('../views/LibraryPage.vue') },
      { path: 'activities', name: 'activities', component: () => import('../views/ActivityPage.vue') },
      { path: 'stats', name: 'stats', component: () => import('../views/StatsPage.vue') },
      { path: 'settings', name: 'settings', component: () => import('../views/SettingsPage.vue') }
    ]
  }
]

const router = createRouter({ history: createWebHashHistory(), routes })

// 沒登入只能看登入頁（記住原本要去哪，登入後帶回去，例如信裡的設定頁連結）；已登入就不用再看登入頁
router.beforeEach(to => {
  const loggedIn = !!authToken()
  if (!to.meta.public && !loggedIn) return { name: 'login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } }
  if (to.name === 'login' && loggedIn) return safeRedirect(to.query.redirect)
})

// 只接受站內路徑，避免被帶到奇怪的地方
export function safeRedirect (path) {
  return typeof path === 'string' && path.startsWith('/') && !path.startsWith('//') ? path : { name: 'overview' }
}

export default router
