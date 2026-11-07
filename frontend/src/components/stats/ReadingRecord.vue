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
    <div class="col-12 col-md-6 col-xl-4">
      <div class="card kpi h-100"><div class="card-body">
        <div class="kpi-label">一字千金</div>
        <template v-if="fun.wordPrice">
          <div class="kpi-value small-value rf-serif">每千字 {{ money(fun.wordPrice.perThousand) }}</div>
          <div class="note">1 元可以買 {{ Math.round(fun.wordPrice.wordsPerDollar).toLocaleString() }} 個字 · 依 {{ fun.wordPrice.books }} 本計算</div>
        </template>
        <div v-else class="note mt-1">需要同時填了字數與金額的書才算得出來。</div>
      </div></div>
    </div>
    <div class="col-12 col-md-6 col-xl-4">
      <div class="card kpi h-100"><div class="card-body">
        <div class="kpi-label">看書速度</div>
        <template v-if="fun.speed">
          <div class="kpi-value small-value rf-serif">每分鐘 {{ Math.round(fun.speed.wpm).toLocaleString() }} 字</div>
          <div class="note">約每天 {{ wan(fun.speed.wpd) }} · 依 {{ fun.speed.books }} 本計算（中位數）</div>
        </template>
        <div v-else class="note mt-1">讀完一本開書到完讀 30 天內、有字數的書後就會出現。開書日期 2026-11-06 起才有，以前讀過的書可以在維護表單補填。</div>
        <button v-if="details.length" type="button" class="btn btn-sm btn-link px-0 mt-1" :aria-expanded="showDetail" @click="showDetail = !showDetail">{{ showDetail ? '收起計算明細' : '查看計算明細' }}</button>
      </div></div>
    </div>
    <div class="col-12 col-md-6 col-xl-4">
      <div class="card kpi h-100"><div class="card-body">
        <div class="kpi-label">讀完書架還要</div>
        <template v-if="fun.backlog?.days">
          <div class="kpi-value small-value rf-serif">{{ fun.backlog.days.toLocaleString() }} 天</div>
          <div class="note">
            未讀 {{ wan(fun.backlog.words) }} · 照每天 {{ Math.round(fun.backlog.avgDaily) }} 分鐘
            <template v-if="fun.backlog.books_without_words">（另有 {{ fun.backlog.books_without_words }} 本沒有字數或是版式，不計）</template>
          </div>
        </template>
        <div v-else-if="fun.backlog && !fun.backlog.books_with_words && !fun.backlog.books_without_words" class="kpi-value small-value rf-serif">全部讀完了！</div>
        <div v-else class="note mt-1">有看書速度、未讀的書有字數，就能估算。</div>
      </div></div>
    </div>
    <div class="col-12 col-md-6 col-xl-4">
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
    <div class="col-12 col-md-6 col-xl-4">
      <div class="card kpi h-100"><div class="card-body">
        <div class="kpi-label">最順 / 最卡的種類</div>
        <template v-if="cats.length">
          <div class="cat-line">
            <span class="tag cheap">最順</span>{{ cats[0].name }}
            <span class="price">平均每分鐘 {{ Math.round(cats[0].avg_wpm).toLocaleString() }} 字 · {{ cats[0].books }} 本</span>
          </div>
          <div v-if="cats.length > 1" class="cat-line">
            <span class="tag dear">最卡</span>{{ cats[cats.length - 1].name }}
            <span class="price">平均每分鐘 {{ Math.round(cats[cats.length - 1].avg_wpm).toLocaleString() }} 字 · {{ cats[cats.length - 1].books }} 本</span>
          </div>
          <div v-if="cats.length > 2" class="note">共 {{ cats.length }} 個分類有排名</div>
          <div v-else-if="cats.length === 1" class="note">目前只有一個分類夠本數，再多讀幾個分類就能比較。</div>
        </template>
        <div v-else class="note mt-1">用看書速度「採計」的書依分類平均，每個分類至少 2 本才排名（未分類不算）。</div>
      </div></div>
    </div>
    <div class="col-12 col-md-6 col-xl-4">
      <div class="card kpi h-100"><div class="card-body">
        <div class="kpi-label">字數富翁</div>
        <template v-if="rich">
          <div class="kpi-value small-value rf-serif text-truncate">{{ rich.author }}</div>
          <div class="note">書架上共 {{ wan(rich.words) }} · {{ rich.books }} 本</div>
          <button type="button" class="btn btn-sm btn-link px-0 mt-1" :aria-expanded="showRanking" @click="showRanking = !showRanking">{{ showRanking ? '收起作家排名' : '查看作家排名' }}</button>
        </template>
        <div v-else class="note mt-1">書填了作者與字數後，就會算出字數最多的作者。</div>
      </div></div>
    </div>
  </div>

  <section v-if="showRanking" class="card mt-3">
    <div class="card-body">
      <div class="d-flex align-items-center mb-2">
        <h2 class="h6 mb-0">字數富翁作家排名</h2>
        <button type="button" class="btn-close btn-sm ms-auto" aria-label="關閉作家排名" @click="showRanking = false"></button>
      </div>
      <p class="small text-secondary">書架上所有有字數的書（讀了沒都算）依作者加總字數；合著的書每位作者都算全本字數，版式與沒填字數的書不算。</p>
      <div class="table-responsive">
        <table class="table table-sm align-middle mb-0 rank-table">
          <thead><tr><th class="rank">名次</th><th>作者</th><th class="text-end">字數</th><th class="text-end">本數</th><th class="text-end">平均每本</th></tr></thead>
          <tbody>
            <tr v-for="r in rankedAuthors.slice(0, rankLimit)" :key="r.author">
              <td class="rank"><span class="medal" :class="`m${r.rank}`">{{ r.rank }}</span></td>
              <td>{{ r.author }}</td>
              <td class="text-end text-nowrap">{{ r.words.toLocaleString() }}</td>
              <td class="text-end">{{ r.books }}</td>
              <td class="text-end text-nowrap">{{ wan(r.words / r.books) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="d-flex align-items-center justify-content-center gap-3 mt-2">
        <span class="small text-secondary">顯示 {{ Math.min(rankLimit, rankedAuthors.length) }} / {{ rankedAuthors.length }} 位</span>
        <button v-if="rankLimit < rankedAuthors.length" type="button" class="btn btn-sm btn-outline-primary" @click="rankLimit += DETAIL_STEP">
          再顯示 {{ Math.min(DETAIL_STEP, rankedAuthors.length - rankLimit) }} 位
        </button>
      </div>
    </div>
  </section>

  <section v-if="showDetail" class="card mt-3">
    <div class="card-body">
      <div class="d-flex align-items-center mb-2">
        <h2 class="h6 mb-0">看書速度計算明細</h2>
        <button type="button" class="btn-close btn-sm ms-auto" aria-label="關閉計算明細" @click="showDetail = false"></button>
      </div>
      <p class="small text-secondary">
        ① 先排除：沒有字數、開書到完讀超過 30 天、期間沒有登錄分鐘、有填時數的天數不到一半（閱讀中的書算到今天，條件一樣；它只分走重疊日子的分鐘，不算自己的速度）。
        ② 只用通過的書分配分鐘：每天登錄的分鐘 ÷ 當天同時在讀的本數。
        ③ 每分鐘不到 50 或超過 1,000 字的排除，排除後重新分配，直到沒有書再被排除。
        ④ 主數字取採計書每分鐘字數的<b>中位數</b>。被排除的書不會分走別人的分鐘，它的「分到分鐘」是假設它也在讀時的參考值。
      </p>
      <p v-if="!details.length" class="text-secondary small mb-0">還沒有填了開書日期的已讀或閱讀中的書。</p>
      <div v-else class="table-responsive">
        <table class="table table-sm align-middle mb-0 detail-table">
          <thead>
            <tr><th>書名</th><th>開書 ～ 完讀 / 今天</th><th class="text-end">天數</th><th class="text-end">有填時數</th><th class="text-end">字數</th><th class="text-end">分到分鐘</th><th class="text-end">同時在讀</th><th class="text-end">每分鐘字數</th><th>結果</th></tr>
          </thead>
          <tbody>
            <tr v-for="d in details.slice(0, detailLimit)" :key="d.id" :class="{ excluded: !d.included && !(d.reading && !d.reason) }">
              <td><button type="button" class="book-link inline" @click="$emit('open', d.id)">{{ d.title }}</button></td>
              <td class="text-nowrap">{{ d.from.slice(5) }} ～ {{ d.to.slice(5) }}<span class="text-secondary">（{{ d.to.slice(0, 4) }}）</span></td>
              <td class="text-end">{{ d.days }}</td>
              <td class="text-end">{{ d.logged_days }} 天</td>
              <td class="text-end">{{ d.words ? d.words.toLocaleString() : '—' }}</td>
              <td class="text-end">{{ d.minutes ? Math.round(d.minutes).toLocaleString() : '—' }}</td>
              <td class="text-end">{{ d.max_concurrent > 1 ? `最多 ${d.max_concurrent} 本` : '—' }}</td>
              <td class="text-end">{{ d.wpm != null ? Math.round(d.wpm).toLocaleString() : '—' }}</td>
              <td class="text-nowrap">
                <span v-if="d.included" class="pill pill-state-read">採計</span>
                <template v-else-if="d.reading">
                  <span class="pill pill-state-reading">閱讀中</span>
                  <span class="small text-secondary ms-1">{{ d.reason ? `被排除：${d.reason}` : '只參與分配' }}</span>
                </template>
                <span v-else class="small text-secondary">{{ d.reason }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="details.length" class="d-flex align-items-center justify-content-center gap-3 mt-2">
        <span class="small text-secondary">顯示 {{ Math.min(detailLimit, details.length) }} / {{ details.length }} 筆</span>
        <button v-if="detailLimit < details.length" type="button" class="btn btn-sm btn-outline-primary" @click="detailLimit += DETAIL_STEP">
          再顯示 {{ Math.min(DETAIL_STEP, details.length - detailLimit) }} 筆
        </button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { formatMinutes, funStats, wan } from '../../utils/stats'

// 閱讀紀錄 KPI（規劃書 4.5，照 1.0）：完讀比例低於 66.7% 顯示朱色
const props = defineProps({ summary: { type: Object, required: true } })
defineEmits(['open']) // 點最划算 / 最貴的書名，攤開那本書

const fun = computed(() => funStats(props.summary))
const details = computed(() => props.summary.speed?.details || [])
const showDetail = ref(false)
// 明細先顯示最新 20 筆（依完讀 / 今天新到舊），按鈕每次多 20 筆；收起後重來
const DETAIL_STEP = 20
const detailLimit = ref(DETAIL_STEP)
watch(showDetail, open => { if (!open) detailLimit.value = DETAIL_STEP })
const cats = computed(() => props.summary.category_speeds || [])
const rich = computed(() => props.summary.word_rich)
// 作家排名：同字數同名次；先顯示前 20 名，按鈕每次多 20 位，收起後重來
const showRanking = ref(false)
const rankLimit = ref(20)
watch(showRanking, open => { if (!open) rankLimit.value = DETAIL_STEP })
const rankedAuthors = computed(() => {
  let rank = 0
  return (props.summary.word_rich_ranking || []).map((r, i, all) => {
    if (i === 0 || all[i - 1].words !== r.words) rank = i + 1
    return { ...r, rank }
  })
})
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
.book-link.inline { display: inline; width: auto; padding: 0; text-decoration: underline; text-decoration-color: var(--rf-line-2); }
.detail-table { font-size: 13px; min-width: 760px; }
.detail-table tr.excluded td { color: var(--rf-ink-2); }
.cat-line { padding-top: 4px; font-size: 14px; }
.cat-line .price { display: block; padding-left: 4px; font-size: 12px; color: var(--rf-ink-2); }
.rank-table { font-size: 13px; }
.rank { width: 56px; }
.medal { display: inline-block; min-width: 24px; text-align: center; border-radius: 12px; font-size: 12px; color: var(--rf-ink-2); }
.medal.m1 { background: #E9D9A6; color: #6E5338; }
.medal.m2 { background: #E1E3E4; color: #4F5458; }
.medal.m3 { background: #E6CBB0; color: #6E4A2C; }
.tag { display: inline-block; padding: 0 6px; margin-right: 4px; border-radius: 8px; font-size: 11px; }
.tag.cheap { background: #E1E7D5; color: #5F6F43; }
.tag.dear { background: #EED9D3; color: #8E3B2F; }
</style>
