<template>
  <div class="d-flex align-items-baseline gap-2 mb-3">
    <h2 class="h5 mb-0">租借中</h2>
    <span v-if="loaded" class="text-secondary small">{{ loans.length }} 筆</span>
    <RouterLink v-if="auth.isAdmin" :to="{ name: 'settings', query: { tab: 'loans' } }" class="small ms-auto">租借管理</RouterLink>
  </div>
  <p v-if="loaded && !loans.length" class="text-secondary small mb-0">
    {{ auth.isAdmin ? '目前沒有申請中或借出中的書。' : '目前沒有借書。到圖書館切到「管理員書架」就能借書。' }}
  </p>
  <ul class="list-unstyled mb-0">
    <li v-for="l in loans" :key="l.id" class="loan">
      <img v-if="l.book_cover" :src="l.book_cover" alt="" class="thumb">
      <span v-else class="thumb blank" :style="{ backgroundColor: color(l), color: spineInk(color(l)) }" aria-hidden="true">{{ l.book_title.slice(0, 1) }}</span>
      <div class="body">
        <div class="d-flex align-items-center gap-2">
          <span class="title">{{ l.book_title }}</span>
          <span class="pill" :class="`pill-${l.status}`">{{ LOAN_LABELS[l.status] }}</span>
        </div>
        <div v-if="auth.isAdmin" class="small text-truncate">{{ l.borrower_name }} <span class="text-secondary">{{ l.borrower_email }}</span></div>
        <div class="small text-secondary">{{ l.status === 'lent' ? `借出 ${formatTime(l.lent_at)}` : `申請 ${formatTime(l.requested_at)}` }}</div>
      </div>
      <div class="buttons">
        <LoanButtons :loan="l" size="sm" @changed="onChanged" />
      </div>
    </li>
  </ul>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import { formatTime, LOAN_LABELS } from '../utils/loan'
import { spineColor, spineInk } from '../utils/spine'
import LoanButtons from './LoanButtons.vue'

// 總覽「租借中」（規劃書 4.2）：管理員看全部申請中與借出中，可核定 / 收回；一般使用者看自己的，可歸還
const emit = defineEmits(['changed'])

const auth = useAuthStore()
const loans = ref([])
const loaded = ref(false)

const color = l => spineColor({ title: l.book_title, spine_color: l.book_spine_color })

async function load () {
  const params = { status: 'active', ...(auth.isAdmin ? { all: true } : {}) }
  loans.value = (await http.get('/loans', { params })).data
  loaded.value = true
}

async function onChanged () {
  await load()
  emit('changed')
}

onMounted(load)
defineExpose({ load })
</script>

<style scoped>
.loan { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-top: 1px solid var(--rf-line); }
.loan:first-child { border-top: 0; padding-top: 0; }
.thumb { flex: none; width: 40px; height: 56px; object-fit: cover; border-radius: 2px; box-shadow: 0 1px 3px rgba(43, 42, 40, .25); }
.thumb.blank { display: inline-flex; align-items: center; justify-content: center; font-family: var(--rf-serif); }
.body { flex: 1; min-width: 0; }
.title { font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.buttons { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 6px; }
@media (max-width: 575.98px) {
  .loan { flex-wrap: wrap; }
  .buttons { width: 100%; }
}
</style>
