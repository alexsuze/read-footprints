<template>
  <div class="d-flex flex-wrap align-items-center gap-2 mb-3">
    <div class="d-flex align-items-center gap-1 text-nowrap">
      <button type="button" class="btn btn-sm btn-outline-primary" @click="goToday">今天</button>
      <button type="button" class="btn btn-sm btn-outline-primary" aria-label="上個月" @click="shift(-1)"><i class="bi bi-chevron-left"></i></button>
      <span class="rf-serif month text-center">{{ year }} 年 {{ month + 1 }} 月</span>
      <button type="button" class="btn btn-sm btn-outline-primary" aria-label="下個月" @click="shift(1)"><i class="bi bi-chevron-right"></i></button>
    </div>
  </div>

  <div class="weekdays"><span v-for="d in WEEKDAYS" :key="d">{{ d }}</span></div>
  <div v-for="week in weeks" :key="week[0]" class="week">
    <span
      v-for="(day, i) in week"
      :key="day"
      class="day"
      :class="{ other: Number(day.slice(5, 7)) !== month + 1, now: day === now }"
      :style="{ gridColumn: i + 1 }"
    >{{ Number(day.slice(8)) }}</span>
    <button
      v-for="bar in weekBars(week, activities)"
      :key="bar.activity.id"
      type="button"
      class="bar"
      :class="{ starts: bar.starts, ends: bar.ends }"
      :style="{
        gridColumn: `${bar.from + 1} / ${bar.to + 2}`,
        gridRow: bar.lane + 2,
        backgroundColor: bar.activity.color,
        color: spineInk(bar.activity.color)
      }"
      :title="bar.activity.name"
      @click="selected = bar.activity"
    >{{ bar.activity.name }}</button>
  </div>

  <p v-if="!selected" class="text-secondary small mt-3 mb-0">點色條查看活動資訊。</p>
  <div v-else class="detail mt-3">
    <div class="d-flex align-items-center gap-2 mb-2">
      <span class="pill pill-active">{{ TYPE_LABELS[selected.type] }}</span>
      <span class="fw-medium me-auto">{{ selected.name }}</span>
      <button type="button" class="btn-close btn-sm" aria-label="關閉活動資訊" @click="selected = null"></button>
    </div>
    <div class="text-secondary small" :class="{ 'mb-2': selected.type === 'marathon' }">
      {{ selected.start_date }} ～ {{ selected.end_date }} · {{ activityStatus(selected, now).label }}
      <template v-if="selected.url"> · <a :href="selected.url" target="_blank" rel="noopener noreferrer">活動網址</a></template>
    </div>
    <MarathonProgress v-if="selected.type === 'marathon'" :activity="selected">
      <AddMinutes v-if="activityStatus(selected, now).key !== 'ended'" :key="selected.id" :activity="selected" />
    </MarathonProgress>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import AddMinutes from './AddMinutes.vue'
import MarathonProgress from './MarathonProgress.vue'
import { useToday } from '../composables/useToday'
import { activityStatus, monthWeeks, TYPE_LABELS, weekBars } from '../utils/activity'
import { spineInk } from '../utils/spine'

// 單月月曆，活動畫成橫跨日期的色條，放在活動紀錄頁的「月曆」檢視（規劃書 4.4）
defineProps({ activities: { type: Array, required: true } })

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']
const now = useToday()
const year = ref(Number(now.value.slice(0, 4)))
const month = ref(Number(now.value.slice(5, 7)) - 1)
const selected = ref(null)
const weeks = computed(() => monthWeeks(year.value, month.value))

function shift (delta) {
  const d = new Date(year.value, month.value + delta, 1)
  year.value = d.getFullYear()
  month.value = d.getMonth()
}

function goToday () {
  year.value = Number(now.value.slice(0, 4))
  month.value = Number(now.value.slice(5, 7)) - 1
}
</script>

<style scoped>
.month { min-width: 104px; }
.weekdays, .week { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); }
.weekdays { font-size: 12px; color: var(--rf-ink-2); text-align: center; margin-bottom: 4px; }
.week { grid-auto-rows: min-content; row-gap: 2px; min-height: 58px; padding-bottom: 6px; border-top: 1px solid var(--rf-line); align-content: start; }
.day { grid-row: 1; padding: 3px 5px; font-size: 12px; }
.day.other { color: #B8B0A0; }
.day.now { color: var(--rf-wood); font-weight: 500; background: #F1E8D6; border-radius: 4px; }
.bar {
  min-width: 0;
  height: 18px;
  padding: 0 5px;
  border: 0;
  font-size: 11px;
  line-height: 18px;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}
.bar.starts { border-top-left-radius: 4px; border-bottom-left-radius: 4px; margin-left: 2px; }
.bar.ends { border-top-right-radius: 4px; border-bottom-right-radius: 4px; margin-right: 2px; }
.bar:focus-visible { outline: 2px solid var(--rf-ink); outline-offset: 1px; }
.detail { padding-top: 12px; border-top: 1px solid var(--rf-line); }
</style>
