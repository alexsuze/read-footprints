<template>
  <p v-if="!books.length" class="text-secondary text-center py-5 mb-0">{{ emptyText }}</p>
  <div v-else class="table-responsive">
    <table class="table table-hover align-middle mb-0 book-table">
      <thead>
        <tr>
          <th>書名</th>
          <th class="d-none d-md-table-cell">作者</th>
          <th class="d-none d-md-table-cell">出版社</th>
          <th class="d-none d-lg-table-cell">分類</th>
          <th>狀態</th>
          <th class="d-none d-md-table-cell">完讀日期</th>
          <template v-if="showPrivate">
            <th class="d-none d-lg-table-cell">購買日期</th>
            <th class="d-none d-lg-table-cell text-end">金額</th>
          </template>
        </tr>
      </thead>
      <tbody>
        <tr v-for="b in books" :key="b.id" tabindex="0" :aria-label="`攤開《${b.title}》`" @click="$emit('select', b)" @keydown.enter="$emit('select', b)">
          <td>
            <div class="d-flex align-items-center gap-2">
              <span class="swatch" :style="{ backgroundColor: spineColor(b) }" aria-hidden="true"></span>
              <div class="min-w-0">
                <div class="title">{{ b.title }}</div>
                <!-- 手機沒有作者欄，放在書名下面 -->
                <div v-if="b.authors?.length" class="small text-secondary d-md-none">{{ b.authors.join('、') }}</div>
              </div>
            </div>
          </td>
          <td class="small d-none d-md-table-cell">{{ (b.authors || []).join('、') || '—' }}</td>
          <td class="small d-none d-md-table-cell">{{ b.publisher || '—' }}</td>
          <td class="small d-none d-lg-table-cell">{{ b.category_name || '未分類' }}</td>
          <td class="text-nowrap">
            <span class="pill" :class="`pill-state-${bookState(b)}`">{{ STATE_LABELS[bookState(b)] }}</span>
            <span v-if="b.loan?.status === 'lent'" class="pill pill-lent ms-1">借出</span>
          </td>
          <td class="small text-nowrap d-none d-md-table-cell">{{ b.read_date || '—' }}</td>
          <template v-if="showPrivate">
            <td class="small text-nowrap d-none d-lg-table-cell">{{ b.buy_date || '—' }}</td>
            <td class="small text-nowrap d-none d-lg-table-cell text-end">{{ b.price != null ? `$${b.price.toLocaleString()}` : '—' }}</td>
          </template>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { bookState, STATE_LABELS } from '../utils/book'
import { spineColor } from '../utils/spine'

// 圖書館的列表模式（與書架模式同一頁資料、同樣每頁本數）。點一列攤開書籍
defineProps({
  books: { type: Array, required: true },
  showPrivate: { type: Boolean, default: false }, // 自己的書架才顯示購買日期與金額
  emptyText: { type: String, default: '這個書架還沒有書' }
})
defineEmits(['select'])
</script>

<style scoped>
.book-table tbody tr { cursor: pointer; }
.book-table tbody tr:focus-visible { outline: 2px solid var(--rf-ink); outline-offset: -2px; }
.swatch { flex: none; width: 6px; height: 26px; border-radius: 2px; }
.title { font-weight: 500; }
.min-w-0 { min-width: 0; }
</style>
