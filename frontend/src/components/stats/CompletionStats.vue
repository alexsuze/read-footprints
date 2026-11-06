<template>
  <div class="sub-tabs mb-3" role="group" aria-label="完讀分析項目">
    <button v-for="t in TABS" :key="t.value" type="button" class="btn btn-sm btn-outline-primary text-nowrap" :class="{ active: tab === t.value }" @click="tab = t.value">
      <i class="bi me-1" :class="t.icon"></i>{{ t.label }}
    </button>
  </div>

  <section v-if="tab === 'report'" class="card"><div class="card-body">
    <div class="d-flex align-items-center mb-2">
      <h2 class="h6 mb-0">{{ mode === 'year' ? '每年' : '每月' }}完讀<span class="text-secondary small fw-normal ms-2">共 {{ readBooks.length }} 本</span></h2>
      <div class="btn-group btn-group-sm ms-auto" role="group" aria-label="期間">
        <button type="button" class="btn btn-outline-primary" :class="{ active: mode === 'month' }" @click="mode = 'month'">月報</button>
        <button type="button" class="btn btn-outline-primary" :class="{ active: mode === 'year' }" @click="mode = 'year'">年報</button>
      </div>
    </div>
    <p v-if="!periods.length" class="text-secondary text-center py-5 mb-0">還沒有完讀紀錄</p>
    <EChart v-else :option="option" :height="340" :min-width="periods.length * (mode === 'month' ? 44 : 64)" />
  </div></section>

  <RankingPanel v-else :books="readBooks" :type="tab" color="#7A8C5C" />
</template>

<script setup>
import { computed, ref } from 'vue'
import EChart from '../EChart.vue'
import RankingPanel from './RankingPanel.vue'
import { barOption } from '../../utils/chart'
import { completedByPeriod, RANKING_TYPES } from '../../utils/stats'

// 完讀分析（規劃書 4.5，照 1.0）：只算已讀的書；排行 + 月 / 年完讀本數
const props = defineProps({ books: { type: Array, required: true } })

const TABS = [...RANKING_TYPES, { value: 'report', label: '月 / 年報', icon: 'bi-calendar3' }]
const tab = ref('category')
const mode = ref('month')

const readBooks = computed(() => props.books.filter(b => b.read))
const periods = computed(() => completedByPeriod(props.books, mode.value))
const option = computed(() => barOption({
  labels: periods.value.map(p => p.period),
  values: periods.value.map(p => p.value),
  color: '#7A8C5C',
  format: v => `${v} 本`,
  rotate: mode.value === 'month' ? 45 : 0
}))
</script>
