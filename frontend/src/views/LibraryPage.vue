<template>
  <div class="d-flex align-items-baseline gap-3 mb-3">
    <h1 class="h3 mb-0">圖書館</h1>
    <span class="text-secondary small">共 {{ total }} 本 · 已讀 {{ readCount }}</span>
  </div>

  <div class="d-flex flex-wrap gap-2 align-items-center mb-3">
    <form class="d-flex gap-2 search" role="search" @submit.prevent="search">
      <input v-model="keyword" type="search" class="form-control" placeholder="搜尋書名、ISBN、作者、譯者、出版社、分類" aria-label="關鍵字">
      <button type="submit" class="btn btn-outline-primary text-nowrap">搜尋</button>
    </form>
    <select v-model="sort" class="form-select w-auto" aria-label="排序欄位" @change="search">
      <option v-for="s in sortOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
    </select>
    <button type="button" class="btn btn-outline-primary" :aria-label="order === 'desc' ? '目前降冪，切換為升冪' : '目前升冪，切換為降冪'" @click="toggleOrder">
      <i class="bi" :class="order === 'desc' ? 'bi-sort-down' : 'bi-sort-up'"></i>
    </button>
    <div class="btn-group" role="group" aria-label="已讀篩選">
      <button v-for="r in READ_FILTERS" :key="r.value" type="button" class="btn btn-outline-primary" :class="{ active: read === r.value }" @click="setRead(r.value)">{{ r.label }}</button>
    </div>
    <div v-if="!auth.isAdmin" class="btn-group" role="group" aria-label="書架">
      <button type="button" class="btn btn-outline-primary" :class="{ active: shelf === 'mine' }" @click="setShelf('mine')">我的書架</button>
      <button type="button" class="btn btn-outline-primary" :class="{ active: shelf === 'admin' }" @click="setShelf('admin')">管理員書架</button>
    </div>
    <div class="ms-auto d-flex gap-2">
      <button type="button" class="btn btn-outline-primary text-nowrap" :disabled="exporting" @click="exportCsv"><i class="bi bi-download me-1"></i>匯出</button>
      <button v-if="isMine" type="button" class="btn btn-primary text-nowrap" @click="openForm(null)"><i class="bi bi-plus-lg me-1"></i>新增書籍</button>
    </div>
  </div>

  <section class="card shelf-card">
    <BookShelf :books="books" :per-row="perRow" :empty-text="emptyText" @select="opened = $event" />
    <div v-if="pages > 1" class="d-flex align-items-center justify-content-center gap-3">
      <button type="button" class="btn btn-sm btn-outline-primary" aria-label="上一架" :disabled="page === 1" @click="go(page - 1)"><i class="bi bi-chevron-left"></i></button>
      <span class="text-secondary rf-serif">第 {{ page }} / {{ pages }} 架</span>
      <button type="button" class="btn btn-sm btn-outline-primary" aria-label="下一架" :disabled="page === pages" @click="go(page + 1)"><i class="bi bi-chevron-right"></i></button>
    </div>
  </section>

  <OpenBook v-if="opened" :book="opened" @close="opened = null" @edit="fromOpen" />
  <BookForm v-if="formOpen" :book="editing" :categories="categories" @close="formOpen = false" @saved="onSaved" />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import BookForm from '../components/BookForm.vue'
import BookShelf from '../components/BookShelf.vue'
import OpenBook from '../components/OpenBook.vue'
import { booksToCsv } from '../utils/csv'

const READ_FILTERS = [{ value: 'all', label: '全部' }, { value: 'unread', label: '未讀' }, { value: 'read', label: '已讀' }]
const SORTS = [
  { value: 'buy_date', label: '購買日期' },
  { value: 'title', label: '書名' },
  { value: 'author', label: '作者' },
  { value: 'publisher', label: '出版社' },
  { value: 'publish_date', label: '出版日期' },
  { value: 'read_date', label: '完讀日期' },
  { value: 'price', label: '金額' },
  { value: 'created_at', label: '建立時間' }
]
const MOBILE = window.matchMedia('(max-width: 767.98px)').matches
const WIDE = window.matchMedia('(min-width: 1400px)').matches
const PER_ROW = MOBILE ? 5 : WIDE ? 20 : 10
const PER_SHELF = PER_ROW * 2 // 上下兩座：手機 10、桌機 20、寬螢幕 40

const auth = useAuthStore()
const perRow = PER_ROW
const books = ref([])
const total = ref(0)
const readCount = ref(0)
const page = ref(1)
const keyword = ref('')
const sort = ref('buy_date')
const order = ref('desc')
const read = ref('all')
const shelf = ref('mine')
const categories = ref([])
const opened = ref(null)
const formOpen = ref(false)
const editing = ref(null)
const exporting = ref(false)

const isMine = computed(() => shelf.value === 'mine')
const emptyText = computed(() => {
  if (keyword.value.trim() || read.value !== 'all') return '找不到符合條件的書'
  return isMine.value ? '書架還空著，按「新增書籍」放上第一本書' : '管理員書架還沒有書'
})
const pages = computed(() => Math.max(1, Math.ceil(total.value / PER_SHELF)))
const sortOptions = computed(() => SORTS.filter(s => isMine.value || s.value !== 'price'))
const query = () => ({ shelf: shelf.value, keyword: keyword.value, read: read.value, sort: sort.value, order: order.value })

async function load () {
  const { data } = await http.get('/books', { params: { ...query(), page: page.value, size: PER_SHELF } })
  books.value = data.items
  total.value = data.total
  readCount.value = data.read_count
}

function go (n) {
  page.value = n
  load()
}
const search = () => go(1)

function toggleOrder () {
  order.value = order.value === 'desc' ? 'asc' : 'desc'
  search()
}

function setRead (value) {
  read.value = value
  search()
}

function setShelf (value) {
  shelf.value = value
  if (value === 'admin' && sort.value === 'price') sort.value = 'buy_date'
  search()
}

function openForm (book) {
  editing.value = book
  formOpen.value = true
}

function fromOpen (book) {
  opened.value = null
  openForm(book)
}

function onSaved () {
  formOpen.value = false
  load()
}

async function exportCsv () {
  exporting.value = true
  try {
    const { data } = await http.get('/books', { params: { ...query(), page: 1, size: 5000 } })
    const url = URL.createObjectURL(new Blob([booksToCsv(data.items, isMine.value)], { type: 'text/csv;charset=utf-8' }))
    const link = document.createElement('a')
    link.href = url
    link.download = `讀跡書單-${new Date().toLocaleDateString('sv')}.csv`
    link.click()
    URL.revokeObjectURL(url)
  } finally {
    exporting.value = false
  }
}

onMounted(async () => {
  await load()
  categories.value = (await http.get('/categories', { silent: true })).data
})
</script>

<style scoped>
.search { flex: 1 1 280px; max-width: 420px; }
.shelf-card { padding: 34px 28px 28px; }
.btn-group .btn { white-space: nowrap; }

@media (max-width: 767.98px) {
  .shelf-card { padding: 28px 14px 20px; }
}
</style>
