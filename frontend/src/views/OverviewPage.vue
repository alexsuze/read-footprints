<template>
  <div class="d-flex align-items-baseline gap-3 mb-3">
    <h1 class="h3 mb-0">總覽</h1>
    <span class="text-secondary small">{{ dateLabel }}</span>
  </div>

  <section class="card mb-3">
    <div class="card-body">
      <div class="d-flex flex-wrap align-items-center gap-2 mb-4">
        <h2 class="h5 mb-0">書架</h2>
        <span class="text-secondary small">近期購買 {{ books.length }} 本</span>
        <div v-if="!auth.isAdmin" class="btn-group ms-auto" role="group" aria-label="書架">
          <button type="button" class="btn btn-sm btn-outline-primary text-nowrap" :class="{ active: shelf === 'mine' }" @click="setShelf('mine')">我的書架</button>
          <button type="button" class="btn btn-sm btn-outline-primary text-nowrap" :class="{ active: shelf === 'admin' }" @click="setShelf('admin')">管理員書架</button>
        </div>
      </div>
      <BookShelf v-if="loaded" :books="books" :per-row="PER_ROW" @select="opened = $event">
        <template #empty>
          <template v-if="shelf === 'mine'">書架還空著，<RouterLink :to="{ name: 'library' }">到圖書館新增第一本書</RouterLink></template>
          <template v-else>管理員書架還沒有書</template>
        </template>
      </BookShelf>
    </div>
  </section>

  <div class="row g-3">
    <div class="col-lg-7">
      <section class="card h-100"><div class="card-body"><TodayCalendar :activities="activities" /></div></section>
    </div>
    <div class="col-lg-5">
      <section class="card h-100">
        <div class="card-body">
          <LoanBoard ref="loanBoard" @changed="loadBooks" />
        </div>
      </section>
    </div>
  </div>

  <OpenBook v-if="opened" :book="opened" readonly @close="opened = null" @changed="loanBoard?.load()" />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import BookShelf from '../components/BookShelf.vue'
import LoanBoard from '../components/LoanBoard.vue'
import OpenBook from '../components/OpenBook.vue'
import TodayCalendar from '../components/TodayCalendar.vue'

const RECENT = 12
const PER_ROW = window.matchMedia('(max-width: 767.98px)').matches ? 6 : RECENT

const auth = useAuthStore()
const dateLabel = new Date().toLocaleDateString('zh-TW', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'short' })
const books = ref([])
const loaded = ref(false)
const activities = ref([])
const shelf = ref('mine')
const opened = ref(null)
const loanBoard = ref(null)

async function loadBooks () {
  const params = { shelf: shelf.value, sort: 'buy_date', order: 'desc', page: 1, size: RECENT }
  books.value = (await http.get('/books', { params })).data.items
  loaded.value = true
}

function setShelf (value) {
  shelf.value = value
  loadBooks()
}

onMounted(async () => {
  loadBooks()
  activities.value = (await http.get('/activities')).data
})
</script>
