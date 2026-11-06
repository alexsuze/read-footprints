<template>
  <Teleport to="body">
    <div class="modal-backdrop show"></div>
    <div class="modal d-block" tabindex="-1" role="dialog" aria-modal="true" :aria-label="book ? '維護書籍' : '新增書籍'" @keydown.esc="$emit('close')">
      <div class="modal-dialog modal-lg modal-dialog-scrollable">
        <form class="modal-content" @submit.prevent="save">
          <div class="modal-header">
            <h2 class="modal-title h5">{{ book ? '維護書籍' : '新增書籍' }}</h2>
            <button type="button" class="btn-close" aria-label="關閉" @click="$emit('close')"></button>
          </div>
          <div class="modal-body">
            <div class="row g-3">
              <div class="col-md-8">
                <label class="form-label" for="bf-title">書名（必填）</label>
                <input id="bf-title" v-model="form.title" class="form-control" required maxlength="200">
              </div>
              <div class="col-md-4">
                <label class="form-label" for="bf-isbn">ISBN</label>
                <input id="bf-isbn" v-model="form.isbn" class="form-control">
              </div>
              <div class="col-md-6">
                <label class="form-label" for="bf-authors">作者（多位用逗號分隔）</label>
                <input id="bf-authors" v-model="form.authors" class="form-control">
              </div>
              <div class="col-md-6">
                <label class="form-label" for="bf-translators">譯者（多位用逗號分隔）</label>
                <input id="bf-translators" v-model="form.translators" class="form-control">
              </div>
              <div class="col-md-4">
                <label class="form-label" for="bf-publisher">出版社</label>
                <input id="bf-publisher" v-model="form.publisher" class="form-control">
              </div>
              <div class="col-md-4">
                <label class="form-label" for="bf-publish-date">出版日期</label>
                <input id="bf-publish-date" v-model="form.publish_date" class="form-control" placeholder="yyyy-MM-dd">
              </div>
              <div class="col-md-4">
                <label class="form-label" for="bf-category">分類</label>
                <select id="bf-category" v-model="form.category_id" class="form-select">
                  <option value="">未分類</option>
                  <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
                </select>
              </div>
              <div class="col-md-4">
                <label class="form-label" for="bf-words">字數</label>
                <input id="bf-words" v-model.number="form.word_count" type="number" min="0" class="form-control">
              </div>
              <div class="col-md-4 d-flex align-items-end">
                <div class="form-check mb-2">
                  <input id="bf-read" v-model="form.read" type="checkbox" class="form-check-input">
                  <label class="form-check-label" for="bf-read">已讀</label>
                </div>
              </div>
              <div class="col-md-4">
                <label class="form-label" for="bf-read-date">完讀日期</label>
                <input id="bf-read-date" v-model="form.read_date" type="date" class="form-control" :disabled="!form.read">
              </div>
              <div class="col-md-4">
                <label class="form-label" for="bf-price">購買金額</label>
                <input id="bf-price" v-model.number="form.price" type="number" min="0" class="form-control">
              </div>
              <div class="col-md-4">
                <label class="form-label" for="bf-buy-date">購買日期</label>
                <input id="bf-buy-date" v-model="form.buy_date" type="date" class="form-control">
              </div>
              <div class="col-md-4">
                <label class="form-label" for="bf-buy-url">購買連結</label>
                <input id="bf-buy-url" v-model="form.buy_url" type="url" class="form-control">
              </div>
              <div class="col-12 d-flex gap-3 align-items-center">
                <img v-if="cover" :src="cover" alt="書封預覽" class="cover-preview">
                <div>
                  <label class="form-label d-block" for="bf-cover">書封</label>
                  <input id="bf-cover" type="file" accept="image/*" class="form-control" @change="pickCover">
                  <button v-if="cover" type="button" class="btn btn-sm btn-link text-danger px-0" @click="removeCover">移除書封</button>
                </div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button v-if="book" type="button" class="btn btn-outline-danger me-auto" :disabled="saving" @click="remove">刪除</button>
            <button type="button" class="btn btn-outline-primary" @click="$emit('close')">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="saving">儲存</button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../api/http'
import { toast } from './AppToast.vue'
import { compressImage } from '../utils/image'

// 新增 / 維護書籍（規劃書 4.3）。book 為 null 時是新增
const props = defineProps({
  book: { type: Object, default: null },
  categories: { type: Array, default: () => [] }
})
const emit = defineEmits(['close', 'saved'])

const b = props.book
const form = reactive({
  title: b?.title ?? '',
  isbn: b?.isbn ?? '',
  authors: (b?.authors ?? []).join(', '),
  translators: (b?.translators ?? []).join(', '),
  publisher: b?.publisher ?? '',
  publish_date: b?.publish_date ?? '',
  category_id: b?.category_id ?? '',
  word_count: b?.word_count ?? '',
  read: b?.read ?? false,
  read_date: b?.read_date ?? '',
  price: b?.price ?? '',
  buy_date: b?.buy_date ?? '',
  buy_url: b?.buy_url ?? ''
})
const cover = ref('')
let coverChanged = false
const saving = ref(false)

const list = text => text.split(/[,，、]/).map(s => s.trim()).filter(Boolean)
const orNull = v => (v === '' ? null : v)

async function pickCover (e) {
  const file = e.target.files[0]
  if (!file) return
  try {
    cover.value = await compressImage(file)
    coverChanged = true
  } catch {
    toast('無法讀取這張圖片', 'danger')
  }
}

function removeCover () {
  cover.value = ''
  coverChanged = true
}

async function save () {
  const body = {
    ...form,
    authors: list(form.authors),
    translators: list(form.translators),
    word_count: orNull(form.word_count),
    price: orNull(form.price),
    read_date: form.read ? form.read_date || new Date().toLocaleDateString('sv') : '',
    cover: coverChanged ? cover.value : null // null = 不動書封
  }
  saving.value = true
  try {
    if (b) await http.put(`/books/${b.id}`, body)
    else await http.post('/books', body)
    toast('已儲存')
    emit('saved')
  } finally {
    saving.value = false
  }
}

async function remove () {
  if (!window.confirm(`確定刪除《${b.title}》？刪除後無法復原。`)) return
  saving.value = true
  try {
    await http.delete(`/books/${b.id}`)
    toast('已刪除')
    emit('saved')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (!b) return
  const { data } = await http.get(`/books/${b.id}`, { silent: true })
  if (!coverChanged) cover.value = data.cover || ''
})
</script>

<style scoped>
.cover-preview { width: 84px; height: 120px; object-fit: cover; border-radius: 3px; box-shadow: 1px 2px 4px rgba(0, 0, 0, .25); }
</style>
