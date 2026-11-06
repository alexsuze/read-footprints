<template>
  <div class="d-flex align-items-baseline gap-2 mb-2">
    <b class="rf-serif now" :class="{ muted }">{{ current.toLocaleString() }}</b>
    <span class="text-secondary">/ {{ goal.toLocaleString() }} 分鐘<template v-if="current >= goal"> · 達標</template></span>
    <slot />
  </div>
  <div class="track" role="progressbar" :aria-valuenow="current" aria-valuemin="0" :aria-valuemax="goal" :aria-label="`${activity.name} 進度`">
    <div class="fill" :style="{ width: `${percent}%`, background: muted ? 'var(--rf-line-2)' : activity.color }"></div>
    <div v-for="s in middleStages" :key="s" class="tick" :style="{ left: `${s * 100 / goal}%` }"></div>
  </div>
  <div class="stages small mt-2">
    <span v-for="(s, i) in stages" :key="i" :class="{ reached: current >= s }">
      {{ i === stages.length - 1 ? '目標' : `第 ${i + 1} 階` }} {{ s.toLocaleString() }} 分<template v-if="current >= s"> ✓</template>
    </span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// 馬拉松進度條，活動卡片與日曆共用。目前分鐘數是手動維護的（規劃書 4.4）
const props = defineProps({
  activity: { type: Object, required: true },
  muted: { type: Boolean, default: false }
})

const goal = computed(() => props.activity.goal_minutes || 0)
const current = computed(() => props.activity.current_minutes || 0)
const stages = computed(() => props.activity.stage_minutes || [])
const middleStages = computed(() => stages.value.slice(0, -1))
const percent = computed(() => (goal.value ? Math.min(100, current.value * 100 / goal.value) : 0))
</script>

<style scoped>
.now { font-size: 28px; color: var(--rf-wood); }
.now.muted { color: var(--rf-ink-2); font-size: 22px; }
.track { position: relative; height: 12px; background: var(--rf-paper-2); border-radius: 6px; }
.fill { position: absolute; inset: 0 auto 0 0; border-radius: 6px; }
.tick { position: absolute; top: -4px; width: 2px; height: 20px; background: var(--rf-wood); }
.stages { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 2px 14px; color: var(--rf-ink-2); }
.stages .reached { color: #5F6F43; }
</style>
