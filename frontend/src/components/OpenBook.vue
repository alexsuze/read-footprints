<template>
  <Teleport to="body">
    <div class="open-overlay" @click.self="$emit('close')" @keydown.esc="$emit('close')">
      <div ref="dialog" class="open-book" role="dialog" aria-modal="true" :aria-label="book.title" tabindex="-1">
        <div class="page left">
          <img v-if="detail?.cover" :src="detail.cover" :alt="`${book.title} 書封`" class="cover">
          <div v-else class="cover blank" :style="{ backgroundColor: spineColor(book), color: spineInk(spineColor(book)) }">
            <span class="rf-serif">{{ book.title }}</span>
            <small>{{ detail ? '尚無書封' : '載入中…' }}</small>
          </div>
        </div>
        <div class="page right">
          <h2 class="rf-serif title">{{ book.title }}</h2>
          <dl class="info">
            <template v-for="row in rows" :key="row[0]">
              <dt>{{ row[0] }}</dt>
              <dd>{{ row[1] || '—' }}</dd>
            </template>
            <dt>狀態</dt>
            <dd>
              <span class="pill" :class="`pill-state-${state}`">{{ stateText }}</span>
              <span v-if="isOwner && book.reading_since" class="small text-secondary ms-2">{{ book.reading_since }} 開書</span>
            </dd>
            <dt>購買連結</dt>
            <dd class="text-truncate">
              <a v-if="safeUrl" :href="safeUrl" target="_blank" rel="noopener noreferrer">{{ safeUrl }}</a>
              <template v-else>—</template>
            </dd>
            <template v-if="book.loan">
              <dt>借閱</dt>
              <dd><span class="pill" :class="`pill-${book.loan.status}`">{{ LOAN_LABELS[book.loan.status] }}</span><span class="small text-secondary ms-2">{{ book.loan.mine ? '你借的' : book.loan.borrower_name || '' }}</span></dd>
            </template>
            <template v-if="isOwner">
              <dt class="own">購買</dt>
              <dd class="own">{{ book.buy_date || '—' }} · {{ book.price != null ? `NT$ ${book.price}` : '—' }}</dd>
            </template>
          </dl>
          <p v-if="isOwner" class="text-secondary small mb-0">金額與購買日期只有書主看得到</p>
          <form v-if="finishing" class="finish" @submit.prevent="setState('read')">
            <label class="small text-secondary" for="ob-read-date">完讀日期</label>
            <input id="ob-read-date" v-model="finishDate" type="date" class="form-control form-control-sm" :max="today" required>
            <button type="submit" class="btn btn-sm btn-primary text-nowrap" :disabled="busy">完讀</button>
            <button type="button" class="btn btn-sm btn-outline-primary" @click="finishing = false">取消</button>
          </form>
          <div class="actions">
            <button type="button" class="btn btn-outline-primary" @click="$emit('close')">關閉</button>
            <template v-if="isOwner && !finishing">
              <button v-if="state === 'unread'" type="button" class="btn btn-outline-primary" :disabled="busy" @click="setState('reading')">開書</button>
              <template v-if="state === 'reading'">
                <button type="button" class="btn btn-outline-danger" :disabled="busy" @click="giveUp">放棄</button>
                <button type="button" class="btn btn-outline-primary" @click="startFinish">讀完了</button>
              </template>
            </template>
            <LoanButtons :book="book" @changed="$emit('changed', $event)" />
            <button v-if="isOwner && !readonly" type="button" class="btn btn-primary" @click="$emit('edit', book)">維護</button>
          </div>
        </div>
        <div class="fold"></div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import { toast } from './AppToast.vue'
import { bookState, readingDay } from '../utils/book'
import { LOAN_LABELS } from '../utils/loan'
import { spineColor, spineInk } from '../utils/spine'
import LoanButtons from './LoanButtons.vue'

// 攤開書籍（規劃書第 3 節）：左頁書封，右頁基本資料。書封只在這裡才下載
const props = defineProps({
  book: { type: Object, required: true },
  readonly: { type: Boolean, default: false } // 總覽只有詳情，沒有維護
})
const emit = defineEmits(['close', 'edit', 'changed'])

const auth = useAuthStore()
const dialog = ref(null)
const detail = ref(null)
const isOwner = computed(() => props.book.user_id === auth.user?.id)
const today = new Date().toLocaleDateString('sv')
const state = computed(() => bookState(props.book))
const stateText = computed(() => {
  if (state.value === 'read') return `完讀 ${props.book.read_date || ''}`
  if (state.value === 'reading') return `閱讀中 · 第 ${readingDay(props.book.reading_since, today) ?? 1} 天`
  return '未讀'
})
const busy = ref(false)
const finishing = ref(false)
const finishDate = ref(today)

// 開書 / 讀完了 / 放棄（規劃書 4.3）：直接改狀態，不用進維護表單
async function setState (next) {
  busy.value = true
  try {
    const body = next === 'read' ? { state: next, date: finishDate.value } : { state: next }
    const { data } = await http.put(`/books/${props.book.id}/state`, body)
    for (const k of ['read', 'read_date', 'reading', 'reading_since']) props.book[k] = data[k]
    finishing.value = false
    toast(next === 'reading' ? '開書了，祝閱讀愉快' : next === 'read' ? '恭喜讀完！' : '已放棄，回到未讀')
    emit('changed')
  } finally {
    busy.value = false
  }
}

function giveUp () {
  if (confirm(`放棄《${props.book.title}》？會回到未讀，開書日期也會清掉。`)) setState('unread')
}

function startFinish () {
  finishDate.value = today
  finishing.value = true
}
const safeUrl = computed(() => (/^https?:\/\//i.test(props.book.buy_url || '') ? props.book.buy_url : ''))
const rows = computed(() => {
  const b = props.book
  return [
    ['作者', (b.authors || []).join('、')],
    ['譯者', (b.translators || []).join('、')],
    ['出版社', b.publisher],
    ['出版日期', b.publish_date],
    ['分類', b.category_name || '未分類'],
    ['ISBN', b.isbn],
    ['字數', b.word_count ? `${b.word_count.toLocaleString()} 字` : '']
  ]
})

onMounted(async () => {
  dialog.value?.focus()
  try {
    detail.value = (await http.get(`/books/${props.book.id}`)).data
  } catch {
    emit('close') // 錯誤訊息由攔截器顯示
  }
})
</script>

<style scoped>
.open-overlay {
  position: fixed;
  inset: 0;
  z-index: 1050;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(43, 42, 40, .35);
  animation: fade .18s;
}
.open-book {
  display: flex;
  width: min(860px, 100%);
  min-height: 520px;
  max-height: 100%;
  position: relative;
  overflow: auto;
  border-radius: 4px;
  box-shadow: 0 24px 50px rgba(30, 20, 10, .45);
  animation: unfold .22s ease-out;
}
.open-book:focus { outline: none; }
.page { width: 50%; min-width: 0; padding: 40px; }
.left {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(90deg, #EFE7D6 0%, var(--rf-card) 12%);
  border-right: 1px solid #D9D0BE;
}
.right {
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: linear-gradient(90deg, var(--rf-card) 88%, #EFE7D6 100%);
}
.cover {
  width: 230px;
  max-width: 100%;
  max-height: 340px;
  object-fit: contain;
  border-radius: 3px;
  box-shadow: 6px 8px 20px rgba(0, 0, 0, .3);
}
.cover.blank {
  height: 330px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 26px 22px;
  font-size: 24px;
  line-height: 1.4;
}
.cover.blank small { font-size: 12px; opacity: .85; }
.title { font-size: 26px; font-weight: 600; margin: 0; overflow-wrap: anywhere; }
.info { display: grid; grid-template-columns: 72px minmax(0, 1fr); gap: 9px 12px; margin: 0; font-size: 13px; }
.info dt { color: var(--rf-ink-2); font-weight: 400; }
.info dd { margin: 0; overflow-wrap: anywhere; }
.info .own { color: var(--rf-wood); }
.finish { margin-top: auto; display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 8px; padding-bottom: 10px; }
.finish .form-control { width: auto; }
.finish + .actions { margin-top: 0; }
.actions { margin-top: auto; display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 10px; }
.fold { position: absolute; top: 0; bottom: 0; left: 50%; width: 2px; background: rgba(0, 0, 0, .12); pointer-events: none; }

@keyframes fade { from { opacity: 0; } }
@keyframes unfold { from { transform: scaleX(.6); opacity: 0; } }

@media (prefers-reduced-motion: reduce) {
  .open-overlay, .open-book { animation: none; }
}
@media (max-width: 767.98px) {
  .open-book { flex-direction: column; min-height: 0; }
  .page { width: 100%; padding: 24px; }
  .left { border-right: 0; border-bottom: 1px solid #D9D0BE; }
  .cover, .cover.blank { width: 150px; height: auto; max-height: 220px; min-height: 200px; }
  .fold { display: none; }
}
</style>
