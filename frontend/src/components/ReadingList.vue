<template>
  <p class="small text-secondary mb-2">閱讀中 {{ books.length }} 本</p>
  <p v-if="!books.length" class="text-secondary small mb-0">還沒開書。點開一本書按「開書」，就會出現在這裡。</p>
  <div v-for="b in books" :key="b.id" class="row-book">
    <div class="d-flex align-items-center gap-2">
      <span class="swatch" :style="{ backgroundColor: spineColor(b), color: spineInk(spineColor(b)) }" aria-hidden="true">{{ b.title.slice(0, 1) }}</span>
      <button type="button" class="title" :title="`攤開《${b.title}》`" @click="$emit('open', b)">{{ b.title }}</button>
      <span class="text-secondary small text-nowrap">第 {{ readingDay(b.reading_since, today) ?? 1 }} 天</span>
      <button v-if="finishing !== b.id" type="button" class="btn btn-sm btn-outline-primary text-nowrap" @click="startFinish(b)">讀完了</button>
    </div>
    <form v-if="finishing === b.id" class="finish" @submit.prevent="finish(b)">
      <label class="small text-secondary" :for="`rl-date-${b.id}`">完讀日期</label>
      <input :id="`rl-date-${b.id}`" v-model="date" type="date" class="form-control form-control-sm" :max="today" required>
      <button type="submit" class="btn btn-sm btn-primary text-nowrap" :disabled="busy">完讀</button>
      <button type="button" class="btn btn-sm btn-outline-primary" @click="finishing = null">取消</button>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import http from '../api/http'
import { toast } from './AppToast.vue'
import { readingDay } from '../utils/book'
import { spineColor, spineInk } from '../utils/spine'

// 總覽活動日曆裡的「閱讀中」（同時通常只有一兩本，用清單比書架省空間）：點書名攤開，「讀完了」選日期直接完讀
defineProps({ books: { type: Array, required: true } })
const emit = defineEmits(['open', 'changed'])

const today = new Date().toLocaleDateString('sv')
const finishing = ref(null)
const date = ref(today)
const busy = ref(false)

function startFinish (b) {
  date.value = today
  finishing.value = b.id
}

async function finish (b) {
  busy.value = true
  try {
    await http.put(`/books/${b.id}/state`, { state: 'read', date: date.value })
    finishing.value = null
    toast('恭喜讀完！')
    emit('changed')
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.row-book { border-left: 4px solid var(--rf-wood); padding: 6px 0 6px 10px; margin-bottom: 8px; }
.swatch { flex: none; width: 22px; height: 30px; border-radius: 2px; display: inline-flex; align-items: center; justify-content: center; font-family: var(--rf-serif); font-size: 12px; box-shadow: 0 1px 2px rgba(43, 42, 40, .25); }
.title { flex: 1; min-width: 0; padding: 0; border: 0; background: none; text-align: left; color: inherit; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.title:hover { color: var(--rf-wood); }
.title:focus-visible { outline: 2px solid var(--rf-ink); outline-offset: 2px; }
.finish { display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 6px; margin-top: 8px; }
.finish .form-control { width: auto; }
</style>
