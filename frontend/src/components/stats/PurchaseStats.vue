<template>
  <div class="sub-tabs mb-3" role="group" aria-label="購買分析項目">
    <button v-for="t in TABS" :key="t.value" type="button" class="btn btn-sm btn-outline-primary text-nowrap" :class="{ active: tab === t.value }" @click="tab = t.value">
      <i class="bi me-1" :class="t.icon"></i>{{ t.label }}
    </button>
  </div>

  <template v-if="tab === 'spending'">
    <div class="row g-3">
      <div v-for="k in kpis" :key="k.label" class="col-6 col-md-4 col-xl">
        <div class="card kpi h-100"><div class="card-body">
          <div class="kpi-label">{{ k.label }}</div>
          <div class="kpi-value rf-serif">{{ k.value }}</div>
        </div></div>
      </div>
    </div>
    <p class="text-secondary small mt-2 mb-0">年均 = 總金額 ÷ 從最早購買年到今年的年數；月均 = 年均 ÷ 12；日均 = 月均 ÷ 30。平均書價只算有填金額的書。</p>
  </template>

  <div v-else-if="tab === 'analysis'" class="row g-3">
    <div class="col-lg-8">
      <section class="card h-100"><div class="card-body">
        <div class="d-flex align-items-center mb-2">
          <h2 class="h6 mb-0">{{ mode === 'year' ? '年度' : '每月' }}花費</h2>
          <div class="btn-group btn-group-sm ms-auto" role="group" aria-label="期間">
            <button type="button" class="btn btn-outline-primary" :class="{ active: mode === 'year' }" @click="mode = 'year'">年</button>
            <button type="button" class="btn btn-outline-primary" :class="{ active: mode === 'month' }" @click="mode = 'month'">月</button>
          </div>
        </div>
        <p v-if="!periods.length" class="text-secondary text-center py-5 mb-0">沒有資料</p>
        <EChart v-else :option="periodOption" :height="340" :min-width="periods.length * (mode === 'month' ? 44 : 64)" />
      </div></section>
    </div>
    <div class="col-lg-4">
      <section class="card h-100"><div class="card-body">
        <h2 class="h6 mb-2">花費列表</h2>
        <div class="table-wrap">
          <table class="table table-sm mb-0">
            <thead><tr><th>{{ mode === 'year' ? '年份' : '月份' }}</th><th class="text-end">金額</th></tr></thead>
            <tbody>
              <tr v-for="p in [...periods].reverse()" :key="p.period"><td>{{ p.period }}</td><td class="text-end">{{ money(p.value) }}</td></tr>
            </tbody>
          </table>
        </div>
      </div></section>
    </div>
  </div>

  <RankingPanel v-else :books="books" :type="tab" />
</template>

<script setup>
import { computed, ref } from 'vue'
import EChart from '../EChart.vue'
import RankingPanel from './RankingPanel.vue'
import { barOption, money } from '../../utils/chart'
import { RANKING_TYPES, spending, spendingByPeriod } from '../../utils/stats'

// 購買分析（規劃書 4.5，照 1.0）：分類 / 出版社 / 作者 / 譯者排行、購買力 KPI、年 / 月花費
const props = defineProps({ books: { type: Array, required: true } })

const TABS = [
  ...RANKING_TYPES,
  { value: 'spending', label: '購買力', icon: 'bi-wallet2' },
  { value: 'analysis', label: '購買力分析', icon: 'bi-graph-up' }
]
const tab = ref('category')
const mode = ref('year')

const s = computed(() => spending(props.books, new Date().getFullYear()))
const kpis = computed(() => [
  { label: '總金額', value: money(s.value.total) },
  { label: '平均書價', value: money(s.value.average) },
  { label: '年均花費', value: money(s.value.yearly) },
  { label: '月均花費', value: money(s.value.monthly) },
  { label: '日均花費', value: money(s.value.daily) }
])
const periods = computed(() => spendingByPeriod(props.books, mode.value))
const periodOption = computed(() => barOption({
  labels: periods.value.map(p => p.period),
  values: periods.value.map(p => p.value),
  format: money,
  rotate: mode.value === 'month' ? 45 : 0,
  avg: mode.value === 'year' ? s.value.yearly : s.value.monthly,
  avgLabel: mode.value === 'year' ? '年均' : '月均'
}))
</script>

<style scoped>
.table-wrap { max-height: 340px; overflow-y: auto; }
.table-wrap thead th { position: sticky; top: 0; background: var(--rf-card); }
</style>
