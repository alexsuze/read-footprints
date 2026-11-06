<template>
  <div class="d-flex flex-wrap align-items-center gap-2 mb-3">
    <div class="btn-group" role="group" aria-label="借閱狀態">
      <button v-for="f in FILTERS" :key="f.value" type="button" class="btn btn-sm btn-outline-primary text-nowrap" :class="{ active: filter === f.value }" @click="filter = f.value">
        {{ f.label }}<span v-if="f.value !== '' && counts[f.value]" class="ms-1">{{ counts[f.value] }}</span>
      </button>
    </div>
    <span class="text-secondary small ms-auto">共 {{ shown.length }} 筆</span>
  </div>

  <div class="card">
    <div class="card-body table-responsive">
      <p v-if="loaded && !shown.length" class="text-secondary text-center py-4 mb-0">沒有借閱紀錄</p>
      <table v-else class="table align-middle mb-0">
        <thead>
          <tr><th>書名</th><th>借書人</th><th>狀態</th><th>申請</th><th>借出</th><th>結束</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="l in shown" :key="l.id">
            <td class="fw-medium">{{ l.book_title }}</td>
            <td>
              <div class="d-flex align-items-center gap-2">
                <img v-if="l.borrower_picture" :src="l.borrower_picture" alt="" class="rounded-circle" width="28" height="28" referrerpolicy="no-referrer">
                <div class="lh-sm">
                  <div>{{ l.borrower_name }}</div>
                  <div class="small text-secondary">{{ l.borrower_email }}</div>
                </div>
              </div>
            </td>
            <td><span class="pill" :class="`pill-${l.status}`">{{ LOAN_LABELS[l.status] }}</span></td>
            <td class="small text-nowrap">{{ formatTime(l.requested_at) }}</td>
            <td class="small text-nowrap">{{ formatTime(l.lent_at) || '—' }}</td>
            <td class="small text-nowrap">
              <template v-if="l.returned_at">{{ formatTime(l.returned_at) }}<div class="text-secondary">{{ endedLabel(l) }}</div></template>
              <template v-else>—</template>
            </td>
            <td class="text-end"><div class="d-inline-flex gap-1"><LoanButtons :loan="l" size="sm" @changed="load" /></div></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../api/http'
import { ENDED_LABELS, formatTime, LOAN_LABELS } from '../utils/loan'
import LoanButtons from './LoanButtons.vue'

// 設定 → 租借管理（規劃書 4.6，管理員）：全部借閱紀錄，可篩選狀態，申請中可核定 / 拒絕，借出中可收回
const FILTERS = [
  { value: '', label: '全部' },
  { value: 'requested', label: '申請中' },
  { value: 'lent', label: '借出中' },
  { value: 'returned', label: '已結束' }
]

const loans = ref([])
const loaded = ref(false)
const filter = ref('')

const shown = computed(() => loans.value.filter(l => !filter.value || l.status === filter.value))
const counts = computed(() => Object.fromEntries(FILTERS.map(f => [f.value, loans.value.filter(l => l.status === f.value).length])))

// 申請中就被收回 = 拒絕
const endedLabel = l => (l.ended_by === 'owner' && !l.lent_at ? '管理員拒絕' : l.ended_by === 'borrower' && !l.lent_at ? '借書人取消' : ENDED_LABELS[l.ended_by])

async function load () {
  loans.value = (await http.get('/loans', { params: { all: true } })).data
  loaded.value = true
}

onMounted(load)
</script>
