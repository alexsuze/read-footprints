<template>
  <div class="row g-3">
    <div class="col-lg-5">
      <section class="card h-100"><div class="card-body">
        <h2 class="h6 mb-3">每日閱讀分鐘</h2>
        <form class="d-flex flex-wrap gap-2 mb-1" @submit.prevent="save">
          <input v-model="form.date" type="date" class="form-control form-control-sm date" :max="now" aria-label="日期" required>
          <input v-model.number="form.minutes" type="number" min="1" max="1440" class="form-control form-control-sm minutes" placeholder="分鐘" aria-label="分鐘數" required>
          <button type="submit" class="btn btn-sm btn-primary text-nowrap" :disabled="saving">{{ existing ? '更新這天' : '新增' }}</button>
        </form>
        <p class="small mb-3" :class="existing ? 'text-warning-emphasis' : 'text-secondary'">
          {{ existing ? `這天已記錄 ${existing.minutes} 分鐘，送出會改成新的分鐘數。` : '同一天只能一筆。' }}
        </p>

        <p v-if="loaded && !logs.length" class="text-secondary text-center py-4 mb-0">還沒有閱讀紀錄</p>
        <table v-else class="table table-sm align-middle mb-2">
          <thead><tr><th>日期</th><th class="text-end">分鐘</th><th></th></tr></thead>
          <tbody>
            <tr v-for="l in pageLogs" :key="l.id">
              <td>{{ l.date }} <span class="text-secondary small">{{ weekday(l.date) }}</span></td>
              <td class="text-end">
                <input v-if="editing === l.id" v-model.number="editMinutes" v-focus type="number" min="1" max="1440" class="form-control form-control-sm minutes ms-auto" aria-label="分鐘數" @keyup.enter="update(l)" @keyup.esc="editing = null">
                <template v-else>{{ l.minutes }}</template>
              </td>
              <td class="text-end text-nowrap">
                <template v-if="editing === l.id">
                  <button type="button" class="btn btn-sm btn-primary" @click="update(l)">儲存</button>
                  <button type="button" class="btn btn-sm btn-outline-primary ms-1" @click="editing = null">取消</button>
                </template>
                <template v-else>
                  <button type="button" class="btn btn-sm btn-outline-primary" @click="startEdit(l)">修改</button>
                  <button type="button" class="btn btn-sm btn-outline-danger ms-1" @click="remove(l)">刪除</button>
                </template>
              </td>
            </tr>
          </tbody>
        </table>
        <PageNav v-if="pages > 1" :page="page" :pages="pages" @go="page = $event" />
      </div></section>
    </div>

    <div class="col-lg-7">
      <section class="card h-100"><div class="card-body">
        <div class="d-flex flex-wrap align-items-center gap-2 mb-2">
          <h2 class="h6 mb-0">{{ mode === 'month' ? `${year} 年每月` : '每年' }}時數</h2>
          <span class="text-secondary small">合計 {{ hours(periodTotal) }}</span>
          <div class="ms-auto d-flex gap-2">
            <select v-if="mode === 'month'" v-model.number="year" class="form-select form-select-sm" aria-label="年份">
              <option v-for="y in years" :key="y" :value="y">{{ y }}</option>
            </select>
            <div class="btn-group btn-group-sm" role="group" aria-label="期間">
              <button type="button" class="btn btn-outline-primary" :class="{ active: mode === 'month' }" @click="mode = 'month'">月</button>
              <button type="button" class="btn btn-outline-primary" :class="{ active: mode === 'year' }" @click="mode = 'year'">年</button>
            </div>
          </div>
        </div>
        <p v-if="!periodTotal" class="text-secondary text-center py-5 mb-0">{{ mode === 'month' ? `${year} 年沒有閱讀紀錄` : '還沒有閱讀紀錄' }}</p>
        <EChart v-else :option="option" :height="340" :min-width="mode === 'month' ? 420 : 0" />
      </div></section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import http from '../../api/http'
import { toast } from '../AppToast.vue'
import EChart from '../EChart.vue'
import PageNav from '../PageNav.vue'
import { barOption } from '../../utils/chart'
import { today } from '../../utils/activity'
import { minutesByMonth, minutesByYear } from '../../utils/stats'

// 時數統計（規劃書 4.5）：上半維護每日分鐘數，下半月 / 年時數長條圖
const emit = defineEmits(['changed'])

const PER_PAGE = 10
const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']
const vFocus = { mounted: el => el.focus() }

const now = today()
const logs = ref([])
const loaded = ref(false)
const saving = ref(false)
// 總覽的「尚未填入閱讀時數」會帶 ?date= 過來，直接選好那一天
const route = useRoute()
const linked = /^\d{4}-\d{2}-\d{2}$/.test(route.query.date || '') && route.query.date <= now ? route.query.date : now
const form = ref({ date: linked, minutes: '' })
const editing = ref(null)
const editMinutes = ref('')
const page = ref(1)
const mode = ref('month')
const year = ref(Number(now.slice(0, 4)))

const existing = computed(() => logs.value.find(l => l.date === form.value.date))
const pages = computed(() => Math.max(1, Math.ceil(logs.value.length / PER_PAGE)))
const pageLogs = computed(() => logs.value.slice((page.value - 1) * PER_PAGE, page.value * PER_PAGE))
const years = computed(() => {
  const set = new Set(logs.value.map(l => Number(l.date.slice(0, 4))))
  set.add(Number(now.slice(0, 4)))
  return [...set].sort((a, b) => b - a)
})
const periods = computed(() => mode.value === 'month' ? minutesByMonth(logs.value, year.value) : minutesByYear(logs.value))
const periodTotal = computed(() => periods.value.reduce((s, p) => s + p.value, 0))
const option = computed(() => barOption({
  labels: periods.value.map(p => mode.value === 'month' ? `${Number(p.period.slice(5))} 月` : p.period),
  values: periods.value.map(p => Math.round(p.value / 6) / 10),
  format: v => `${v} 小時`
}))

watch(pages, p => { if (page.value > p) page.value = p })

const weekday = date => `週${WEEKDAYS[new Date(`${date}T00:00:00`).getDay()]}`
const hours = minutes => `${(minutes / 60).toFixed(1)} 小時`

async function load () {
  logs.value = (await http.get('/reading-logs')).data
  loaded.value = true
}

async function save () {
  saving.value = true
  try {
    if (existing.value) await http.put(`/reading-logs/${existing.value.id}`, { minutes: form.value.minutes })
    else await http.post('/reading-logs', form.value)
    toast(existing.value ? '已更新' : '已新增', 'success')
    form.value = { date: now, minutes: '' }
    await load()
    emit('changed')
  } finally {
    saving.value = false
  }
}

function startEdit (l) {
  editing.value = l.id
  editMinutes.value = l.minutes
}

async function update (l) {
  await http.put(`/reading-logs/${l.id}`, { minutes: editMinutes.value })
  editing.value = null
  await load()
  emit('changed')
}

async function remove (l) {
  if (!confirm(`刪除 ${l.date} 的紀錄（${l.minutes} 分鐘）？`)) return
  await http.delete(`/reading-logs/${l.id}`)
  await load()
  emit('changed')
}

onMounted(load)
</script>

<style scoped>
.date { width: 150px; }
.minutes { width: 90px; }
.form-select { width: auto; }
</style>
