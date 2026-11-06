<template>
  <div class="card" style="max-width: 640px">
    <div class="card-body">
      <div class="d-flex align-items-center gap-3">
        <div class="me-auto">
          <div class="fw-medium">匯入 1.0 資料</div>
          <div class="text-secondary small">從 book_admin 匯入書籍、分類、閱讀時數、活動，全部歸到管理員。可重跑，已匯入的會跳過。</div>
        </div>
        <button type="button" class="btn btn-sm btn-primary text-nowrap" :disabled="running" @click="run">{{ running ? '匯入中…' : '開始匯入' }}</button>
      </div>
      <table v-if="result" class="table table-sm mt-3 mb-0">
        <thead><tr><th>項目</th><th>匯入</th><th>跳過</th></tr></thead>
        <tbody>
          <tr v-for="r in rows" :key="r.label"><td>{{ r.label }}</td><td>{{ r.count.imported }}</td><td>{{ r.count.skipped }}</td></tr>
        </tbody>
      </table>
      <p v-if="result" class="text-secondary small mt-2 mb-0">無法解析主色的書封：{{ result.undecodable_covers }} 本，書脊改用書名配色。</p>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import http from '../api/http'
import { toast } from './AppToast.vue'

// 階段 5 確認匯入完成後移除（規劃書第 9 節）
const running = ref(false)
const result = ref(null)
const rows = computed(() => [
  { label: '分類', count: result.value.categories },
  { label: '書籍', count: result.value.books },
  { label: '閱讀時數', count: result.value.reading_logs },
  { label: '活動', count: result.value.activities }
])

async function run () {
  running.value = true
  try {
    result.value = (await http.post('/admin/import-legacy', null, { timeout: 600000 })).data
    toast('匯入完成')
  } finally {
    running.value = false
  }
}
</script>
