<template>
  <div class="card" style="max-width: 560px">
    <div class="card-body">
      <div class="d-flex align-items-center gap-3 mb-3">
        <img v-if="user?.picture" :src="user.picture" alt="" class="rounded-circle" width="64" height="64" referrerpolicy="no-referrer">
        <span v-else class="avatar rf-serif" aria-hidden="true">{{ (user?.name || user?.email || '?').slice(0, 1) }}</span>
        <div class="min-w-0">
          <div class="fw-medium fs-5">{{ user?.name }}</div>
          <div class="text-secondary small text-break">{{ user?.email }}</div>
          <span class="pill pill-active mt-1">{{ auth.isAdmin ? '管理員' : '一般使用者' }}</span>
        </div>
      </div>
      <dl class="facts mb-0">
        <div><dt>加入日期</dt><dd>{{ joined || '—' }}</dd></div>
        <div><dt>書目數</dt><dd>{{ counts ? `${counts.total.toLocaleString()} 本` : '—' }}</dd></div>
        <div><dt>已讀完</dt><dd>{{ counts ? `${counts.read.toLocaleString()} 本` : '—' }}</dd></div>
        <div><dt>{{ auth.isAdmin ? '借出中' : '借閱中' }}</dt><dd>{{ loans == null ? '—' : `${loans} 本` }}</dd></div>
      </dl>
      <p class="text-secondary small mt-3 mb-0">名稱與大頭貼來自 Google 帳號，在 Google 修改後重新登入就會更新。</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

// 設定 → 個人資料（規劃書 4.6）：Google 資料唯讀，加上自己的書目數與加入日期
const auth = useAuthStore()
const user = ref(auth.user)
const counts = ref(null)
const loans = ref(null)

const joined = computed(() => (user.value?.created_at ? new Date(user.value.created_at).toLocaleDateString('sv') : ''))

onMounted(async () => {
  const [me, books, active] = await Promise.all([
    http.get('/auth/me'),
    http.get('/books', { params: { shelf: 'mine', page: 1, size: 1 } }),
    http.get('/loans', { params: auth.isAdmin ? { all: true, status: 'lent' } : { status: 'active' } })
  ])
  user.value = me.data
  counts.value = { total: books.data.total, read: books.data.read_count }
  loans.value = active.data.length
})
</script>

<style scoped>
.avatar { width: 64px; height: 64px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; background: var(--rf-wood); color: var(--rf-paper); font-size: 26px; }
.min-w-0 { min-width: 0; }
.facts { display: grid; grid-template-columns: repeat(auto-fit, minmax(110px, 1fr)); gap: 12px; padding-top: 14px; border-top: 1px solid var(--rf-line); }
.facts dt { font-size: 12px; font-weight: 400; color: var(--rf-ink-2); }
.facts dd { margin: 0; font-family: var(--rf-serif); font-size: 18px; }
</style>
