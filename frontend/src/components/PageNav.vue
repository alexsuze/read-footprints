<template>
  <div class="d-flex align-items-center justify-content-center gap-2">
    <button type="button" class="btn btn-sm btn-outline-primary" :aria-label="`上一${unit}`" :disabled="page <= 1" @click="go(page - 1)"><i class="bi bi-chevron-left"></i></button>
    <span class="text-secondary small text-nowrap" :class="{ 'rf-serif': unit === '架' }">
      第
      <input
        v-model="draft"
        type="text"
        inputmode="numeric"
        class="form-control form-control-sm d-inline-block page-input"
        :aria-label="`輸入第幾${unit}，按 Enter 跳過去`"
        :title="`輸入數字後按 Enter`"
        @keydown.enter.prevent="commit"
        @blur="draft = String(page)"
        @focus="$event.target.select()"
      >
      / {{ pages }} {{ unit }}
    </span>
    <button type="button" class="btn btn-sm btn-outline-primary" :aria-label="`下一${unit}`" :disabled="page >= pages" @click="go(page + 1)"><i class="bi bi-chevron-right"></i></button>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { clampPage } from '../utils/book'

// 翻頁列：左右箭頭，中間可輸入頁數按 Enter 跳頁（超出範圍自動拉回、不是數字就恢復）
const props = defineProps({
  page: { type: Number, required: true },
  pages: { type: Number, required: true },
  unit: { type: String, default: '頁' }
})
const emit = defineEmits(['go'])

const draft = ref(String(props.page))
watch(() => props.page, p => { draft.value = String(p) })

function go (n) {
  if (n >= 1 && n <= props.pages && n !== props.page) emit('go', n)
}

function commit (e) {
  const n = clampPage(draft.value, props.pages)
  draft.value = String(n ?? props.page)
  if (n != null) go(n)
  e.target.blur()
}
</script>

<style scoped>
.page-input { width: 3.4em; padding: 1px 4px; text-align: center; }
</style>
