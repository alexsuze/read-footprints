<template>
  <form v-if="adding" class="d-flex gap-1 ms-2" @submit.prevent="submit">
    <input v-model.number="minutes" v-focus type="number" min="1" class="form-control form-control-sm minutes" aria-label="增加的分鐘數" required>
    <button type="submit" class="btn btn-sm btn-primary text-nowrap" :disabled="saving">確定</button>
    <button type="button" class="btn btn-sm btn-outline-primary text-nowrap" @click="adding = false">取消</button>
  </form>
  <button v-else type="button" class="btn btn-sm btn-outline-primary ms-2 text-nowrap" @click="start">＋ 登錄分鐘</button>
</template>

<script setup>
import { ref } from 'vue'
import http from '../api/http'

// 馬拉松登錄分鐘：輸入本次閱讀分鐘，加到目前分鐘數（活動紀錄頁與總覽共用）
const props = defineProps({ activity: { type: Object, required: true } })

const vFocus = { mounted: el => el.focus() }
const adding = ref(false)
const saving = ref(false)
const minutes = ref('')

function start () {
  minutes.value = ''
  adding.value = true
}

async function submit () {
  saving.value = true
  try {
    const a = props.activity
    const { data } = await http.put(`/activities/${a.id}`, { ...a, current_minutes: (a.current_minutes || 0) + minutes.value })
    Object.assign(a, data)
    adding.value = false
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.minutes { width: 84px; }
</style>
