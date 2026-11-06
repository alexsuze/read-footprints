<template>
  <div class="row g-3 mb-3">
    <div v-for="k in books" :key="k.label" class="col-6 col-md-3">
      <div class="card kpi h-100"><div class="card-body">
        <div class="kpi-label">{{ k.label }}</div>
        <div class="kpi-value rf-serif" :style="{ color: k.color }">{{ k.value }}</div>
      </div></div>
    </div>
  </div>
  <div class="row g-3">
    <div v-for="k in time" :key="k.label" class="col-12 col-md-4">
      <div class="card kpi h-100"><div class="card-body">
        <div class="kpi-label">{{ k.label }}</div>
        <div class="kpi-value small-value rf-serif">{{ k.value }}</div>
      </div></div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatMinutes } from '../../utils/stats'

// 閱讀紀錄 KPI（規劃書 4.5，照 1.0）：完讀比例低於 66.7% 顯示朱色
const props = defineProps({ summary: { type: Object, required: true } })

const ratio = computed(() => (props.summary.total_books ? props.summary.read_books * 100 / props.summary.total_books : 0))
const books = computed(() => [
  { label: '總書目數', value: props.summary.total_books.toLocaleString() },
  { label: '已讀完', value: props.summary.read_books.toLocaleString() },
  { label: '完讀比例', value: `${ratio.value.toFixed(1)}%`, color: !props.summary.total_books ? undefined : ratio.value < 66.7 ? 'var(--rf-vermilion)' : 'var(--rf-matcha)' },
  { label: '本年完讀', value: props.summary.read_this_year.toLocaleString() }
])
const time = computed(() => [
  { label: '總閱讀時數', value: formatMinutes(props.summary.total_minutes) },
  { label: '平均每日閱讀', value: `${(props.summary.reading_days ? props.summary.total_minutes / props.summary.reading_days : 0).toFixed(1)} 分鐘` },
  { label: '閱讀天數', value: `${props.summary.reading_days.toLocaleString()} 天` }
])
</script>
