<template>
  <div class="card" style="max-width: 560px">
    <div class="card-body">
      <p class="small text-secondary">勾選要顯示的欄位，用箭頭調整順序。圖書館的列表模式與匯出 CSV 都會照這個設定；手機上的列表固定只顯示書名與狀態。</p>
      <p v-if="!items" class="text-secondary small mb-0">載入中…</p>
      <ul v-else class="list-group list-group-flush mb-3">
        <li class="list-group-item bg-transparent d-flex align-items-center gap-2 px-0">
          <input id="col-title" class="form-check-input mt-0" type="checkbox" checked disabled>
          <label class="me-auto" for="col-title">書名 <span class="small text-secondary">（固定顯示在第一欄）</span></label>
        </li>
        <li v-for="(c, i) in items" :key="c.key" class="list-group-item bg-transparent d-flex align-items-center gap-2 px-0">
          <input :id="`col-${c.key}`" v-model="c.on" class="form-check-input mt-0" type="checkbox">
          <label class="me-auto" :class="{ 'text-secondary': !c.on }" :for="`col-${c.key}`">
            {{ c.label }}<span v-if="c.private" class="small text-secondary ms-1">（只在自己的書架顯示）</span>
          </label>
          <button type="button" class="btn btn-sm btn-outline-primary" :disabled="i === 0" :aria-label="`${c.label}往上移`" @click="move(i, -1)"><i class="bi bi-arrow-up"></i></button>
          <button type="button" class="btn btn-sm btn-outline-primary" :disabled="i === items.length - 1" :aria-label="`${c.label}往下移`" @click="move(i, 1)"><i class="bi bi-arrow-down"></i></button>
        </li>
      </ul>
      <div v-if="items" class="d-flex gap-2">
        <button type="button" class="btn btn-outline-primary" @click="reset">恢復預設</button>
        <button type="button" class="btn btn-primary ms-auto" :disabled="saving || !dirty" @click="save">儲存</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../api/http'
import { toast } from './AppToast.vue'
import { useAuthStore } from '../stores/auth'
import { COLUMNS, DEFAULT_COLUMNS } from '../utils/columns'

// 設定 → 報表欄位：選擇圖書館列表與 CSV 要顯示的欄位與順序，存在帳號（手機、電腦共用）
const auth = useAuthStore()
const saving = ref(false)

// 勾選的欄位照設定順序排在前面，沒勾的照預設順序接在後面
function build (keys) {
  const on = keys.map(k => COLUMNS.find(c => c.key === k)).filter(Boolean)
  const off = COLUMNS.filter(c => !keys.includes(c.key))
  return [...on.map(c => ({ ...c, on: true })), ...off.map(c => ({ ...c, on: false }))]
}

const saved = () => auth.user?.list_columns ?? DEFAULT_COLUMNS
const items = ref(null)
const picked = computed(() => (items.value || []).filter(c => c.on).map(c => c.key))
const dirty = computed(() => picked.value.join() !== saved().join())

function move (i, delta) {
  const list = items.value
  ;[list[i], list[i + delta]] = [list[i + delta], list[i]]
}

function reset () {
  items.value = build(DEFAULT_COLUMNS)
}

// 先跟後端拿最新設定（可能在別台裝置改過）再顯示
onMounted(async () => {
  try {
    await auth.refreshUser()
  } finally {
    items.value = build(saved())
  }
})

async function save () {
  saving.value = true
  try {
    const { data } = await http.put('/auth/me/list-columns', { list_columns: picked.value })
    auth.setUser(data)
    items.value = build(saved())
    toast('已儲存報表欄位')
  } finally {
    saving.value = false
  }
}
</script>
