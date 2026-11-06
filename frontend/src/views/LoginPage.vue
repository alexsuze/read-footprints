<template>
  <div class="login d-flex align-items-center justify-content-center">
    <div class="card login-card text-center p-4 p-sm-5">
      <div class="mark rf-serif mx-auto">跡</div>
      <h1 class="rf-serif mt-3 mb-1">讀跡</h1>
      <p class="text-secondary small mb-4">read-footprints · 把每一本書的足跡留下</p>

      <div v-if="message" class="alert py-2 small text-start" :class="pending ? 'alert-warning' : 'alert-danger'" role="alert">{{ message }}</div>

      <div ref="buttonEl" class="d-flex justify-content-center" style="min-height: 44px"></div>
      <div v-if="busy" class="spinner-border spinner-border-sm text-primary mx-auto mt-2" role="status"><span class="visually-hidden">登入中</span></div>
      <div v-if="!clientId" class="text-danger small mt-2">尚未設定 VITE_GOOGLE_CLIENT_ID，無法登入</div>

      <p class="text-secondary small mt-4 mb-0 lh-lg">第一次登入會送出使用申請，<br>管理員核准後即可開始使用。</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { safeRedirect } from '../router'
import { useAuthStore } from '../stores/auth'

const GSI_SRC = 'https://accounts.google.com/gsi/client'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
const buttonEl = ref(null)
const message = ref('')
const pending = ref(false)
const busy = ref(false)

// Google 登入元件只需要載入一次
function loadGsi () {
  if (window.google?.accounts?.id) return Promise.resolve()
  return new Promise((resolve, reject) => {
    const s = document.createElement('script')
    s.src = GSI_SRC
    s.async = true
    s.onload = resolve
    s.onerror = () => reject(new Error('無法載入 Google 登入，請檢查網路後重新整理'))
    document.head.appendChild(s)
  })
}

async function onCredential ({ credential }) {
  busy.value = true
  message.value = ''
  try {
    await auth.login(credential)
    router.replace(safeRedirect(route.query.redirect))
  } catch (e) {
    pending.value = e.response?.status === 403 && /核准/.test(e.response?.data?.detail || '')
    message.value = e.response?.data?.detail || e.message
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  if (!clientId) return
  try {
    await loadGsi()
    window.google.accounts.id.initialize({ client_id: clientId, callback: onCredential })
    window.google.accounts.id.renderButton(buttonEl.value, { theme: 'outline', size: 'large', shape: 'rectangular', text: 'signin_with', locale: 'zh-TW', width: 300 })
  } catch (e) {
    message.value = e.message
  }
})
</script>

<style scoped>
.login { min-height: 100vh; padding: 24px 16px; background: var(--rf-paper); }
.login-card { width: 100%; max-width: 420px; }
.mark { width: 64px; height: 64px; border: 1px solid var(--rf-wood); border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 26px; color: var(--rf-wood); }
h1 { font-size: 30px; letter-spacing: 8px; }
</style>
