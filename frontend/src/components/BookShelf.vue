<template>
  <p v-if="!books.length" class="text-secondary text-center py-5 mb-0">{{ emptyText }}</p>
  <div v-for="(row, i) in rows" :key="i" class="shelf">
    <div
      v-for="book in row"
      :key="book.id"
      class="spine-slot"
      :class="{ open: active === book.id }"
      :style="{ flexBasis: `${spineWidth(book.word_count)}px` }"
    >
      <div class="book" :style="{ height: `${spineHeight(book.title)}px` }">
        <button
          type="button"
          class="spine"
          :class="{ read: book.read }"
          :title="book.title"
          :style="{ backgroundColor: spineColor(book), color: spineInk(spineColor(book)) }"
          @click="choose(book)"
        >
          <span class="spine-title">{{ book.title }}</span>
        </button>
        <span v-if="book.read" class="spine-tag">完讀</span>
      </div>
      <div v-if="active === book.id" class="spine-menu">
        <slot name="menu" :book="book" :close="close" />
      </div>
    </div>
  </div>
  <div v-if="active" class="spine-backdrop" @click="close"></div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { spineColor, spineHeight, spineInk, spineWidth } from '../utils/spine'

// 總覽與圖書館共用（規劃書第 3 節）。有 menu 插槽且 hasMenu 時，點書先彈按鈕，否則直接 select
const props = defineProps({
  books: { type: Array, required: true },
  perRow: { type: Number, default: 10 },
  hasMenu: { type: Boolean, default: false },
  emptyText: { type: String, default: '這個書架還沒有書' }
})
const emit = defineEmits(['select'])

const active = ref(null)
const close = () => { active.value = null }

const rows = computed(() => {
  const out = []
  for (let i = 0; i < props.books.length; i += props.perRow) out.push(props.books.slice(i, i + props.perRow))
  return out
})

function choose (book) {
  if (props.hasMenu) active.value = active.value === book.id ? null : book.id
  else emit('select', book)
}
</script>

<style scoped>
.shelf {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  min-height: 196px;
  margin-bottom: 46px;
  padding: 0 22px;
  position: relative;
  background: linear-gradient(180deg, rgba(139, 107, 74, .06), rgba(139, 107, 74, .14));
  border-radius: 4px 4px 0 0;
}
.shelf::before {
  content: "";
  position: absolute;
  inset: auto 0 0;
  height: 10px;
  background: linear-gradient(180deg, rgba(0, 0, 0, .18), rgba(0, 0, 0, 0));
  pointer-events: none;
}
.shelf::after {
  content: "";
  position: absolute;
  left: -6px;
  right: -6px;
  bottom: -18px;
  height: 18px;
  border-radius: 2px;
  background: linear-gradient(180deg, #B8956B 0%, #A07D57 40%, #8B6B4A 100%);
  box-shadow: 0 12px 14px -8px rgba(60, 40, 20, .5), inset 0 1px 0 rgba(255, 255, 255, .35);
}

/* 位置與抽出動畫放外層，已讀的灰階只作用在書脊本體，完讀標籤保持綠色 */
.spine-slot { flex: 0 1 44px; min-width: 18px; position: relative; }
.spine-slot.open { z-index: 20; }
.book { position: relative; transition: transform .18s; }
.spine-slot:hover .book, .spine-slot.open .book { transform: translateY(-18px) translateX(-1px); }

.spine {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 26px 0 22px;
  border: 0;
  border-radius: 3px 3px 1px 1px;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  background-image: linear-gradient(90deg, rgba(0, 0, 0, .28) 0%, rgba(0, 0, 0, .08) 10%, rgba(255, 255, 255, .14) 22%, rgba(255, 255, 255, 0) 45%, rgba(0, 0, 0, .05) 75%, rgba(0, 0, 0, .3) 100%);
  box-shadow: 1px 0 0 rgba(0, 0, 0, .25), 2px 2px 3px rgba(0, 0, 0, .25), inset 0 1px 0 rgba(255, 255, 255, .25);
  transition: box-shadow .18s;
}
.spine-slot:hover .spine { box-shadow: 1px 0 0 rgba(0, 0, 0, .25), 6px 10px 12px rgba(0, 0, 0, .3), inset 0 1px 0 rgba(255, 255, 255, .25); }
.spine:focus-visible { outline: 2px solid var(--rf-ink); outline-offset: 1px; }
.spine::before, .spine::after {
  content: "";
  position: absolute;
  left: 4px;
  right: 4px;
  height: 1px;
  background: currentColor;
  opacity: .5;
  box-shadow: 0 3px 0 currentColor;
}
.spine::before { top: 11px; }
.spine::after { bottom: 13px; }
.spine.read { filter: saturate(.25) brightness(.85) contrast(.95); }

.spine-title {
  writing-mode: vertical-rl;
  max-height: 100%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-family: var(--rf-serif);
  font-weight: 600;
  font-size: 12px;
  letter-spacing: 2px;
}

.spine-tag {
  position: absolute;
  inset: 0 0 auto;
  padding: 3px 0;
  border-radius: 3px 3px 0 0;
  background: var(--rf-matcha);
  color: #fff;
  font-size: 10px;
  text-align: center;
  letter-spacing: 2px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, .3);
  pointer-events: none;
  overflow: hidden;
  white-space: nowrap;
}

.spine-menu {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 24px);
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
  padding: 6px;
  background: var(--rf-card);
  border: 1px solid var(--rf-line-2);
  border-radius: 6px;
  box-shadow: 0 4px 10px rgba(60, 40, 20, .15);
  white-space: nowrap;
}
.spine-backdrop { position: fixed; inset: 0; z-index: 10; }

@media (max-width: 767.98px) {
  .shelf { padding: 0 10px; gap: 4px; }
}
</style>
