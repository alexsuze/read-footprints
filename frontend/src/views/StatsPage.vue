<template>
  <h1 class="h3">閱讀統計</h1>
  <ul class="nav nav-tabs mb-3 flex-nowrap tabs-scroll">
    <li v-for="t in TABS" :key="t.key" class="nav-item">
      <a href="#" class="nav-link text-nowrap" :class="{ active: tab === t.key }" @click.prevent="tab = t.key">{{ t.label }}</a>
    </li>
  </ul>

  <template v-if="tab === 'record' || tab === 'streak'">
    <template v-if="summary">
      <ReadingRecord v-if="tab === 'record'" :summary="summary" />
      <ReadingStreak v-else :days="summary.streak_days" />
    </template>
  </template>
  <HoursStats v-else-if="tab === 'hours'" @changed="summary = null" />
  <template v-else-if="books">
    <PurchaseStats v-if="tab === 'purchase'" :books="books" />
    <CompletionStats v-else :books="books" />
  </template>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../api/http'
import CompletionStats from '../components/stats/CompletionStats.vue'
import HoursStats from '../components/stats/HoursStats.vue'
import PurchaseStats from '../components/stats/PurchaseStats.vue'
import ReadingRecord from '../components/stats/ReadingRecord.vue'
import ReadingStreak from '../components/stats/ReadingStreak.vue'

// 閱讀統計五個分頁（規劃書 4.5），只統計自己的資料。分頁記在網址 ?tab=
const TABS = [
  { key: 'record', label: '閱讀紀錄' },
  { key: 'hours', label: '時數統計' },
  { key: 'purchase', label: '購買分析' },
  { key: 'completion', label: '完讀分析' },
  { key: 'streak', label: '閱讀狀況' }
]
const route = useRoute()
const router = useRouter()
const valid = t => TABS.some(x => x.key === t)
const tab = ref(valid(route.query.tab) ? route.query.tab : 'record')
const summary = ref(null)
const books = ref(null)

// 需要時才載入；時數有變動就清掉摘要，下次切回來重抓
async function ensureData (t) {
  if ((t === 'record' || t === 'streak') && !summary.value) {
    summary.value = (await http.get('/stats/summary')).data
  } else if ((t === 'purchase' || t === 'completion') && !books.value) {
    const params = { shelf: 'mine', sort: 'buy_date', order: 'asc', page: 1, size: 5000 }
    books.value = (await http.get('/books', { params })).data.items
  }
}

watch(tab, t => {
  router.replace({ query: { ...route.query, tab: t } })
  ensureData(t)
  // 手機上分頁列可左右捲，把目前分頁捲進畫面
  nextTick(() => document.querySelector('.tabs-scroll .active')?.scrollIntoView({ block: 'nearest', inline: 'nearest' }))
}, { immediate: true })
watch(() => route.query.tab, t => { if (valid(t)) tab.value = t })
</script>
