<template>
  <div class="row g-3">
    <div class="col-lg-6">
      <section class="card h-100"><div class="card-body text-center py-4">
        <div class="kpi-label">連續閱讀</div>
        <div class="streak rf-serif"><span>{{ days.toLocaleString() }}</span> 天</div>
        <span class="badge-level" :style="{ background: m.current.color }"><i class="bi me-1" :class="m.current.icon"></i>{{ m.current.name === '無' ? '尚未達到等級' : `${m.current.name}級` }}</span>
        <div class="mt-4 text-start">
          <div class="track" role="progressbar" :aria-valuenow="m.progress" aria-valuemin="0" aria-valuemax="100" aria-label="往下一級的進度">
            <div class="fill" :style="{ width: `${m.progress}%`, background: (m.next || m.current).color }"></div>
          </div>
          <div class="d-flex justify-content-between small text-secondary mt-1">
            <span>{{ m.current.name }} {{ m.current.days }} 天</span>
            <span v-if="m.next">再 {{ m.next.days - days }} 天升到{{ m.next.name }}（{{ m.next.days }} 天）</span>
            <span v-else>已達最高等級</span>
          </div>
        </div>
        <p class="text-secondary small mt-3 mb-0">從昨天往前算，每天至少 {{ STREAK_MINUTES }} 分鐘才算連續。</p>
      </div></section>
    </div>
    <div class="col-lg-6">
      <section class="card h-100"><div class="card-body">
        <h2 class="h6 mb-3">里程碑</h2>
        <ol class="ladder list-unstyled mb-0">
          <li v-for="(level, i) in MILESTONES.slice(1)" :key="level.name" :class="{ reached: i + 1 <= m.index, now: i + 1 === m.index }">
            <span class="dot" :style="{ background: i + 1 <= m.index ? level.color : 'transparent', borderColor: level.color }"><i class="bi" :class="level.icon"></i></span>
            <span class="me-auto">{{ level.name }}</span>
            <span class="text-secondary small">{{ level.days.toLocaleString() }} 天</span>
            <i v-if="i + 1 <= m.index" class="bi bi-check-lg ms-2 check"></i>
          </li>
        </ol>
      </div></section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { milestone, MILESTONES } from '../../utils/stats'

// 閱讀狀況（規劃書 4.5，照 1.0 首頁）：連續閱讀天數與里程碑
const props = defineProps({ days: { type: Number, required: true } })

const STREAK_MINUTES = 20
const m = computed(() => milestone(props.days))
</script>

<style scoped>
.streak { font-size: 20px; color: var(--rf-ink-2); margin: 4px 0 10px; }
.streak span { font-size: 64px; line-height: 1; color: var(--rf-wood); }
.badge-level { display: inline-block; padding: 4px 14px; border-radius: 14px; color: #FBF8F1; font-size: 14px; }
.track { height: 12px; background: var(--rf-paper-2); border-radius: 6px; overflow: hidden; }
.fill { height: 100%; border-radius: 6px; }
.ladder li { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid var(--rf-line); color: var(--rf-ink-2); }
.ladder li:last-child { border-bottom: 0; }
.ladder li.reached { color: var(--rf-ink); }
.ladder li.now { font-weight: 500; }
.dot { width: 30px; height: 30px; border-radius: 50%; border: 2px solid; display: inline-flex; align-items: center; justify-content: center; font-size: 14px; color: var(--rf-ink-2); }
.reached .dot { color: #FBF8F1; }
.check { color: var(--rf-matcha); }
</style>
