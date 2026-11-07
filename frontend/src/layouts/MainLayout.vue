<template>
  <div class="layout d-flex flex-wrap">
    <nav class="side d-flex flex-column">
      <div class="d-flex align-items-center justify-content-between">
        <div>
          <div class="brand rf-serif">讀跡</div>
          <div class="brand-sub">read-footprints</div>
        </div>
        <button class="btn btn-sm d-md-none" aria-label="選單" @click="open = !open"><i class="bi bi-list fs-4"></i></button>
      </div>
      <ul class="nav flex-column mt-3" :class="{ 'd-none d-md-flex': !open }" @click="open = false">
        <li v-for="m in menu" :key="m.name" class="nav-item">
          <router-link :to="{ name: m.name }" class="nav-link d-flex align-items-center gap-2" exact-active-class="on">
            <i class="bi" :class="m.icon"></i>{{ m.label }}
            <span v-if="m.name === 'settings' && auth.pendingCount" class="badge rounded-pill text-bg-danger ms-auto">{{ auth.pendingCount }}</span>
          </router-link>
        </li>
      </ul>
      <div class="user mt-auto d-flex align-items-center gap-2" :class="{ 'd-none d-md-flex': !open }">
        <img v-if="auth.user?.picture" :src="auth.user.picture" alt="" class="avatar" referrerpolicy="no-referrer">
        <div class="avatar rf-serif d-flex align-items-center justify-content-center" v-else>{{ (auth.user?.name || '?')[0] }}</div>
        <div class="flex-grow-1 small text-truncate">{{ auth.user?.name }}<br><span class="text-secondary" style="font-size: 11px">{{ auth.isAdmin ? '管理員' : auth.user?.email }}</span></div>
        <button class="btn btn-sm btn-link text-secondary p-1" aria-label="登出" title="登出" @click="auth.logout()"><i class="bi bi-box-arrow-right"></i></button>
      </div>
    </nav>
    <main class="content flex-grow-1 p-3 p-md-4">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const open = ref(false)
const menu = [
  { name: 'overview', label: '總覽', icon: 'bi-house-door' },
  { name: 'library', label: '圖書館', icon: 'bi-bookshelf' },
  { name: 'activities', label: '活動紀錄', icon: 'bi-calendar3' },
  { name: 'stats', label: '閱讀統計', icon: 'bi-bar-chart-line' },
  { name: 'settings', label: '設定', icon: 'bi-gear' }
]
onMounted(() => {
  auth.loadPending().catch(() => {})
  auth.refreshUser().catch(() => {}) // 在別台裝置改過的設定（例如報表欄位）也帶過來
})
</script>

<style scoped>
.layout { min-height: 100vh; }
.side { flex: 1 1 220px; max-width: 220px; background: var(--rf-paper-2); border-right: 1px solid #D9D0BE; padding: 24px 16px 18px; }
.content { flex: 999 1 560px; min-width: 0; }
.brand { font-size: 22px; letter-spacing: 6px; margin-left: 10px; }
.brand-sub { font-size: 11px; color: var(--rf-ink-2); letter-spacing: 2px; margin-left: 10px; }
.nav-link { color: var(--rf-ink); border-left: 3px solid transparent; border-radius: 6px; padding: 10px 12px; }
.nav-link.on { background: var(--rf-paper); color: var(--rf-wood); border-left-color: var(--rf-wood); }
.user { border-top: 1px solid #D9D0BE; padding-top: 12px; }
.avatar { width: 34px; height: 34px; border-radius: 50%; background: var(--rf-wood); color: var(--rf-paper); object-fit: cover; flex: none; }
@media (max-width: 767.98px) {
  .layout { align-content: flex-start; } /* 側欄換行到上方時不要被撐高 */
  .side { max-width: none; flex-basis: 100%; border-right: 0; border-bottom: 1px solid #D9D0BE; padding: 14px 16px; }
}
</style>
