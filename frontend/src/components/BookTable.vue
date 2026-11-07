<template>
  <p v-if="!books.length" class="text-secondary text-center py-5 mb-0">{{ emptyText }}</p>
  <div v-else class="table-responsive">
    <table class="table table-hover align-middle mb-0 book-table">
      <thead>
        <tr>
          <th v-for="c in shown" :key="c.key" :class="{ 'text-end': c.align === 'end' }" :aria-sort="ariaSort(c)">
            <button v-if="c.sort" type="button" class="sort-btn" :class="{ on: sort === c.sort }" :title="`依${c.label}排序`" @click="clickSort(c)">
              {{ c.label }}<i class="bi ms-1" :class="sortIcon(c)"></i>
            </button>
            <template v-else>{{ c.label }}</template>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="b in books" :key="b.id" tabindex="0" :aria-label="`攤開《${b.title}》`" @click="$emit('select', b)" @keydown.enter="$emit('select', b)">
          <td v-for="c in shown" :key="c.key" :class="cellClass(c)">
            <div v-if="c.key === 'title'" class="d-flex align-items-center gap-2">
              <span class="swatch" :style="{ backgroundColor: spineColor(b) }" aria-hidden="true"></span>
              <div class="min-w-0">
                <div class="title">{{ b.title }}</div>
                <!-- 手機沒有作者欄，放在書名下面 -->
                <div v-if="compact && b.authors?.length" class="small text-secondary">{{ b.authors.join('、') }}</div>
              </div>
            </div>
            <template v-else-if="c.key === 'state'">
              <span class="pill" :class="`pill-state-${bookState(b)}`">{{ STATE_LABELS[bookState(b)] }}</span>
              <span v-if="b.loan?.status === 'lent'" class="pill pill-lent ms-1">借出</span>
            </template>
            <template v-else-if="c.key === 'price'">{{ b.price != null ? `$${b.price.toLocaleString()}` : '—' }}</template>
            <!-- 字數加千分位，0 = 版式；CSV 有字數的仍輸出純數字 -->
            <template v-else-if="c.key === 'word_count'">{{ wordCountText(b.word_count) }}</template>
            <template v-else-if="c.key === 'buy_url'">
              <a v-if="/^https?:\/\//i.test(b.buy_url || '')" :href="b.buy_url" target="_blank" rel="noopener noreferrer" @click.stop>連結</a>
              <template v-else>—</template>
            </template>
            <template v-else>{{ display(c.text(b)) }}</template>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { bookState, STATE_LABELS, wordCountText } from '../utils/book'
import { spineColor } from '../utils/spine'

// 圖書館的列表模式（與書架模式同一頁資料、同樣每頁本數）。欄位照設定 → 報表欄位，點欄位標題排序，點一列攤開書籍
const props = defineProps({
  books: { type: Array, required: true },
  columns: { type: Array, required: true }, // resolveColumns() 的結果
  sort: { type: String, default: '' },
  order: { type: String, default: 'desc' },
  emptyText: { type: String, default: '這個書架還沒有書' }
})
const emit = defineEmits(['select', 'sort'])

// 手機放不下多欄，固定只留書名（作者放下面）與狀態
const compact = window.matchMedia('(max-width: 767.98px)').matches
const shown = computed(() => (compact ? props.columns.filter(c => c.key === 'title' || c.key === 'state') : props.columns))

const display = v => (v == null || v === '' ? '—' : v)
const cellClass = c => ({
  small: !['title', 'state'].includes(c.key),
  'text-nowrap': ['state', 'price', 'publish_date', 'reading_since', 'read_date', 'buy_date', 'created_at', 'word_count'].includes(c.key),
  'text-end': c.align === 'end'
})

// 再點同一欄就反向；換欄時文字欄預設升冪、日期與金額預設降冪
function clickSort (c) {
  if (props.sort === c.sort) emit('sort', { sort: c.sort, order: props.order === 'desc' ? 'asc' : 'desc' })
  else emit('sort', { sort: c.sort, order: ['title', 'author', 'publisher'].includes(c.sort) ? 'asc' : 'desc' })
}
const sortIcon = c => (props.sort !== c.sort ? 'bi-chevron-expand text-secondary' : props.order === 'desc' ? 'bi-caret-down-fill' : 'bi-caret-up-fill')
const ariaSort = c => (c.sort && props.sort === c.sort ? (props.order === 'desc' ? 'descending' : 'ascending') : undefined)
</script>

<style scoped>
.book-table tbody tr { cursor: pointer; }
.book-table tbody tr:focus-visible { outline: 2px solid var(--rf-ink); outline-offset: -2px; }
.swatch { flex: none; width: 6px; height: 26px; border-radius: 2px; }
.title { font-weight: 500; }
.min-w-0 { min-width: 0; }
.sort-btn { padding: 0; border: 0; background: none; color: inherit; font-weight: inherit; white-space: nowrap; }
.sort-btn.on { color: var(--rf-wood); }
.sort-btn:hover { color: var(--rf-wood); }
.sort-btn .bi { font-size: 11px; }
.sort-btn:focus-visible { outline: 2px solid var(--rf-ink); outline-offset: 2px; }
</style>
