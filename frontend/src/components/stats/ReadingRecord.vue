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

  <h2 class="h6 mt-4 mb-3">趣味數據</h2>
  <div class="row g-3">
    <div class="col-12 col-md-6 col-xl-3">
      <div class="card kpi h-100"><div class="card-body">
        <div class="kpi-label">一字千金</div>
        <template v-if="fun.wordPrice">
          <div class="kpi-value small-value rf-serif">每千字 {{ money(fun.wordPrice.perThousand) }}</div>
          <div class="note">1 元可以買 {{ Math.round(fun.wordPrice.wordsPerDollar).toLocaleString() }} 個字 · 依 {{ fun.wordPrice.books }} 本計算</div>
        </template>
        <div v-else class="note mt-1">需要同時填了字數與金額的書才算得出來。</div>
      </div></div>
    </div>
    <div class="col-12 col-md-6 col-xl-3">
      <div class="card kpi h-100"><div class="card-body">
        <div class="kpi-label">看書速度</div>
        <template v-if="fun.speed">
          <div class="kpi-value small-value rf-serif">每分鐘 {{ Math.round(fun.speed.wpm).toLocaleString() }} 字</div>
          <div class="note">約每天 {{ wan(fun.speed.wpd) }} · 依 {{ fun.speed.books }} 本計算</div>
        </template>
        <div v-else class="note mt-1">讀完一本開書到完讀 30 天內、有字數的書後就會出現。開書日期 2026-11-06 起才有，以前讀過的書可以在維護表單補填。</div>
      </div></div>
    </div>
    <div class="col-12 col-md-6 col-xl-3">
      <div class="card kpi h-100"><div class="card-body">
        <div class="kpi-label">讀完書架還要</div>
        <template v-if="fun.backlog?.days">
          <div class="kpi-value small-value rf-serif">{{ fun.backlog.days.toLocaleString() }} 天</div>
          <div class="note">
            未讀 {{ wan(fun.backlog.words) }} · 照每天 {{ Math.round(fun.backlog.avgDaily) }} 分鐘
            <template v-if="fun.backlog.books_without_words">（另有 {{ fun.backlog.books_without_words }} 本沒填字數不計）</template>
          </div>
        </template>
        <div v-else-if="fun.backlog && !fun.backlog.books_with_words && !fun.backlog.books_without_words" class="kpi-value small-value rf-serif">全部讀完了！</div>
        <div v-else class="note mt-1">有看書速度、未讀的書有字數，就能估算。</div>
      </div></div>
    </div>
    <div class="col-12 col-md-6 col-xl-3">
      <div class="card kpi h-100"><div class="card-body">
        <div class="kpi-label">最划算 / 最貴的一本</div>
        <template v-if="fun.wordPrice?.cheapest">
          <button type="button" class="book-link" @click="$emit('open', fun.wordPrice.cheapest.id)">
            <span class="line"><span class="tag cheap">划算</span>《{{ fun.wordPrice.cheapest.title }}》</span><span class="price">每千字 {{ money(fun.wordPrice.cheapest.perThousand) }}</span>
          </button>
          <button v-if="fun.wordPrice.priciest.id !== fun.wordPrice.cheapest.id" type="button" class="book-link" @click="$emit('open', fun.wordPrice.priciest.id)">
            <span class="line"><span class="tag dear">最貴</span>《{{ fun.wordPrice.priciest.title }}》</span><span class="price">每千字 {{ money(fun.wordPrice.priciest.perThousand) }}</span>
          </button>
        </template>
        <div v-else class="note mt-1">需要同時填了字數與金額的書。</div>
      </div></div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatMinutes, funStats, wan } from '../../utils/stats'

// 閱讀紀錄 KPI（規劃書 4.5，照 1.0）：完讀比例低於 66.7% 顯示朱色
const props = defineProps({ summary: { type: Object, required: true } })
defineEmits(['open']) // 點最划算 / 最貴的書名，攤開那本書

const fun = computed(() => funStats(props.summary))
// 每千字的錢通常是個位數，留一位小數
const money = v => `$${v < 100 ? v.toFixed(1) : Math.round(v).toLocaleString()}`

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

<style scoped>
.note { font-size: 12px; color: var(--rf-ink-2); margin-top: 2px; }
.book-link { display: block; width: 100%; padding: 4px 0 0; border: 0; background: none; text-align: left; color: inherit; font-size: 14px; }
.book-link .line { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.book-link .price { display: block; padding-left: 4px; font-size: 12px; color: var(--rf-ink-2); }
.book-link:hover { color: var(--rf-wood); }
.book-link:focus-visible { outline: 2px solid var(--rf-ink); outline-offset: 2px; }
.tag { display: inline-block; padding: 0 6px; margin-right: 4px; border-radius: 8px; font-size: 11px; }
.tag.cheap { background: #E1E7D5; color: #5F6F43; }
.tag.dear { background: #EED9D3; color: #8E3B2F; }
</style>
