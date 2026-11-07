<template>
  <h2 class="h5 mb-3">活動日曆</h2>
  <div class="today-wrap">
    <div class="today-col">
      <div class="tearoff">
        <div class="band" aria-hidden="true">
          <span v-for="i in 4" :key="i" class="ring"></span>
          <span class="rf-serif month">{{ monthLabel }}</span>
        </div>
        <div class="page">
          <span class="visually-hidden">今天 {{ now }}</span>
          <span class="rf-serif day" aria-hidden="true">{{ Number(now.slice(8)) }}</span>
          <span class="weekday">{{ weekday }}</span>
          <RouterLink v-if="onlyYesterday" :to="hoursLink(missing[0])" class="missing mt-1">尚未填入閱讀時數</RouterLink>
        </div>
      </div>
      <ul v-if="missing.length && !onlyYesterday" class="missing-list list-unstyled mb-0">
        <li v-for="d in missing" :key="d">
          <RouterLink :to="hoursLink(d)" class="missing"><i class="bi bi-exclamation-circle me-1"></i>{{ d.slice(5) }} 尚未填入閱讀時數</RouterLink>
        </li>
      </ul>
    </div>

    <div class="ongoing">
      <p class="small text-secondary mb-2">進行中 {{ ongoing.length }} 個活動</p>
      <p v-if="!ongoing.length" class="text-secondary small mb-0">
        目前沒有進行中的活動，<RouterLink :to="{ name: 'activities' }">前往活動紀錄</RouterLink>
      </p>
      <div v-for="a in ongoing" :key="a.id" class="item" :style="{ borderLeftColor: a.color }">
        <button type="button" class="head" :aria-expanded="opened === a.id" @click="toggle(a.id)">
          <span class="name">{{ a.name }}</span>
          <span class="text-secondary small text-nowrap">剩 {{ daysLeftOf(a) }} 天</span>
          <i class="bi small text-secondary" :class="opened === a.id ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
        </button>
        <div v-if="a.type === 'marathon' && opened !== a.id" class="mini" aria-hidden="true">
          <div :style="{ width: `${percent(a)}%`, background: a.color }"></div>
        </div>
        <div v-if="opened === a.id" class="detail">
          <div class="text-secondary small mb-2">
            <span class="pill pill-active me-1">{{ TYPE_LABELS[a.type] }}</span>
            {{ a.start_date }} ～ {{ a.end_date }}
            <template v-if="a.url"> · <a :href="a.url" target="_blank" rel="noopener noreferrer">活動網址</a></template>
          </div>
          <MarathonProgress v-if="a.type === 'marathon'" :activity="a">
            <AddMinutes :activity="a" />
          </MarathonProgress>
        </div>
      </div>

      <div v-if="reading" class="reading">
        <ReadingList :books="reading" @open="$emit('open-book', $event)" @changed="$emit('changed')" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import AddMinutes from './AddMinutes.vue'
import ReadingList from './ReadingList.vue'
import MarathonProgress from './MarathonProgress.vue'
import { useToday } from '../composables/useToday'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import { daysLeft, TYPE_LABELS } from '../utils/activity'
import { missingLogDates, shiftDate } from '../utils/stats'

// 總覽的撕頁日曆：只顯示今天與進行中的活動，點活動展開資訊，馬拉松可直接登錄分鐘（規劃書 4.2）
// 前 7 天（不含今天）沒填閱讀時數會提醒；只缺昨天時直接寫在日曆上
// 活動下方列出閱讀中的書（2026-11-07 從獨立區塊併進來）
const props = defineProps({
  activities: { type: Array, required: true },
  reading: { type: Array, default: null } // 閱讀中的書；null = 還沒載入，先不顯示
})
defineEmits(['open-book', 'changed'])

const WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
const now = useToday()
const opened = ref(null)
const auth = useAuthStore()
const logDates = ref(null)

// 加入之前的日子不提醒（新帳號第一天不會冒出 7 條）
const joined = auth.user?.created_at ? new Date(auth.user.created_at).toLocaleDateString('sv') : ''
const missing = computed(() => (logDates.value ? missingLogDates(logDates.value, now.value).filter(d => d >= joined) : []))
const onlyYesterday = computed(() => missing.value.length === 1 && missing.value[0] === shiftDate(now.value, -1))
const hoursLink = d => ({ name: 'stats', query: { tab: 'hours', date: d } })

// 換日時重抓
watch(now, async today => {
  const params = { from: shiftDate(today, -7), to: shiftDate(today, -1) }
  const { data } = await http.get('/reading-logs', { params, silent: true })
  logDates.value = data.map(l => l.date)
}, { immediate: true })

const date = computed(() => new Date(`${now.value}T00:00:00`))
const monthLabel = computed(() => date.value.toLocaleDateString('en-US', { month: 'long' }))
const weekday = computed(() => WEEKDAYS[date.value.getDay()])
const ongoing = computed(() => props.activities
  .filter(a => a.start_date <= now.value && a.end_date >= now.value)
  .sort((a, b) => a.end_date.localeCompare(b.end_date)))

const daysLeftOf = a => daysLeft(a, now.value)
const percent = a => (a.goal_minutes ? Math.min(100, (a.current_minutes || 0) * 100 / a.goal_minutes) : 0)

function toggle (id) {
  opened.value = opened.value === id ? null : id
}

</script>

<style scoped>
.today-wrap { display: flex; gap: 24px; align-items: flex-start; }
.today-col { flex: 0 0 150px; }
.tearoff { filter: drop-shadow(0 2px 3px rgba(43, 42, 40, .15)); }
.band {
  position: relative;
  height: 44px;
  background: var(--rf-vermilion);
  border-radius: 8px 8px 0 0;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 5px;
}
.ring {
  position: absolute;
  top: -7px;
  width: 7px;
  height: 18px;
  background: var(--rf-card);
  border: 1px solid var(--rf-line-2);
  border-radius: 4px;
}
.ring:nth-child(1) { left: 18%; }
.ring:nth-child(2) { left: 38%; }
.ring:nth-child(3) { left: 58%; }
.ring:nth-child(4) { left: 78%; }
.month { color: #FBF3EA; font-size: 17px; }
.page {
  background: var(--rf-card);
  border: 1px solid var(--rf-line);
  border-top: 0;
  border-radius: 0 0 6px 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0 12px;
  box-shadow: 3px 3px 0 -1px var(--rf-card), 3px 3px 0 0 var(--rf-line-2);
}
.day { font-size: 72px; line-height: 1.1; font-weight: 600; color: var(--rf-ink); }
.weekday { font-size: 14px; color: var(--rf-ink-2); letter-spacing: 2px; }
.ongoing { flex: 1; min-width: 0; }
.item { border-left: 4px solid; padding: 6px 0 8px 10px; margin-bottom: 8px; }
.head {
  display: flex;
  width: 100%;
  align-items: baseline;
  gap: 8px;
  padding: 0;
  border: 0;
  background: none;
  text-align: left;
  color: inherit;
}
.head:focus-visible { outline: 2px solid var(--rf-ink); outline-offset: 2px; }
.name { flex: 1; min-width: 0; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mini { height: 4px; margin-top: 6px; background: var(--rf-paper-2); border-radius: 2px; overflow: hidden; }
.mini div { height: 100%; }
.detail { margin-top: 8px; }
.reading { margin-top: 18px; padding-top: 14px; border-top: 1px solid var(--rf-line); }
.missing { font-size: 12px; color: var(--rf-vermilion); text-decoration: none; }
.missing:hover { text-decoration: underline; }
.missing-list { margin-top: 12px; }
.missing-list li { padding: 1px 0; }
@media (max-width: 575.98px) {
  .today-wrap { flex-direction: column; align-items: stretch; }
  .today-col { flex: none; width: 170px; align-self: center; }
}
</style>
