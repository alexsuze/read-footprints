<template>
  <form v-if="adding" class="d-flex flex-wrap align-items-center gap-1 ms-2" @submit.prevent="submit">
    <label class="small text-secondary text-nowrap" :for="inputId">累計已讀</label>
    <input :id="inputId" v-model.number="minutes" v-focus type="number" min="0" class="form-control form-control-sm minutes" :title="'輸入活動頁面上顯示的累計已讀分鐘'" required>
    <span class="small text-secondary">分鐘</span>
    <button type="submit" class="btn btn-sm btn-primary text-nowrap" :disabled="saving">確定</button>
    <button type="button" class="btn btn-sm btn-outline-primary text-nowrap" @click="adding = false">取消</button>
  </form>
  <button v-else type="button" class="btn btn-sm btn-outline-primary ms-2 text-nowrap" @click="start">＋ 登錄分鐘</button>
</template>

<script setup>
import { ref } from 'vue'
import http from '../api/http'
import { toast } from './AppToast.vue'

// 馬拉松登錄分鐘（2026-11-11 改）：輸入活動上顯示的「累計已讀分鐘」，直接取代目前分鐘數，
// 不再是把當天分鐘加上去（活動網站常常只給總數）。活動紀錄頁與總覽共用
const props = defineProps({ activity: { type: Object, required: true } })

const vFocus = { mounted: el => { el.focus(); el.select() } }
const inputId = `am-${props.activity.id}`
const adding = ref(false)
const saving = ref(false)
const minutes = ref('')

// 預先帶入目前的累計分鐘，改成新的總數就好
function start () {
  minutes.value = props.activity.current_minutes || 0
  adding.value = true
}

async function submit () {
  saving.value = true
  try {
    const a = props.activity
    const before = a.current_minutes || 0
    const { data } = await http.put(`/activities/${a.id}`, { ...a, current_minutes: minutes.value })
    Object.assign(a, data)
    adding.value = false
    const diff = data.current_minutes - before
    toast(`已更新為 ${data.current_minutes.toLocaleString()} 分鐘${diff ? `（${diff > 0 ? '+' : ''}${diff.toLocaleString()}）` : ''}`)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.minutes { width: 96px; }
</style>
