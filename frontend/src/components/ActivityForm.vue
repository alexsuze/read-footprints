<template>
  <Teleport to="body">
    <div class="modal-backdrop show"></div>
    <div class="modal d-block" tabindex="-1" role="dialog" aria-modal="true" :aria-label="activity ? '編輯活動' : '新增活動'" @keydown.esc="$emit('close')">
      <div class="modal-dialog modal-dialog-scrollable">
        <form class="modal-content" @submit.prevent="save">
          <div class="modal-header">
            <h2 class="modal-title h5">{{ activity ? '編輯活動' : '新增活動' }}</h2>
            <button type="button" class="btn-close" aria-label="關閉" @click="$emit('close')"></button>
          </div>
          <div class="modal-body">
            <div class="row g-3">
              <div class="col-8">
                <label class="form-label" for="af-type">種類</label>
                <select id="af-type" v-model="form.type" class="form-select">
                  <option v-for="(label, value) in TYPE_LABELS" :key="value" :value="value">{{ label }}</option>
                </select>
              </div>
              <div class="col-4">
                <label class="form-label" for="af-color">顏色</label>
                <input id="af-color" v-model="form.color" type="color" class="form-control form-control-color w-100">
              </div>
              <div class="col-12">
                <label class="form-label" for="af-name">名稱（必填）</label>
                <input id="af-name" v-model="form.name" class="form-control" required maxlength="50">
              </div>
              <div class="col-6">
                <label class="form-label" for="af-start">開始日期</label>
                <input id="af-start" v-model="form.start_date" type="date" class="form-control" required>
              </div>
              <div class="col-6">
                <label class="form-label" for="af-end">結束日期</label>
                <input id="af-end" v-model="form.end_date" type="date" class="form-control" required :min="form.start_date">
              </div>
              <div class="col-12">
                <label class="form-label" for="af-url">活動網址</label>
                <input id="af-url" v-model="form.url" type="url" class="form-control" placeholder="https://">
              </div>
              <template v-if="form.type === 'marathon'">
                <div class="col-6">
                  <label class="form-label" for="af-goal">目標分鐘數</label>
                  <input id="af-goal" v-model.number="form.goal_minutes" type="number" min="1" class="form-control" required>
                </div>
                <div class="col-6">
                  <label class="form-label" for="af-current">目前分鐘數</label>
                  <input id="af-current" v-model.number="form.current_minutes" type="number" min="0" class="form-control">
                </div>
                <div class="col-6">
                  <label class="form-label" for="af-stage1">第 1 階（可不填）</label>
                  <input id="af-stage1" v-model.number="form.stage1" type="number" min="1" class="form-control">
                </div>
                <div class="col-6">
                  <label class="form-label" for="af-stage2">第 2 階（可不填）</label>
                  <input id="af-stage2" v-model.number="form.stage2" type="number" min="1" class="form-control">
                </div>
                <p class="text-secondary small mb-0">最後一階自動等於目標分鐘數。</p>
              </template>
            </div>
          </div>
          <div class="modal-footer">
            <button v-if="activity" type="button" class="btn btn-outline-danger me-auto" :disabled="saving" @click="remove">刪除</button>
            <button type="button" class="btn btn-outline-primary" @click="$emit('close')">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="saving">儲存</button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { reactive, ref } from 'vue'
import http from '../api/http'
import { toast } from './AppToast.vue'
import { TYPE_LABELS } from '../utils/activity'

// 新增 / 編輯活動（規劃書 4.4）。activity 為 null 時是新增
const props = defineProps({ activity: { type: Object, default: null } })
const emit = defineEmits(['close', 'saved'])

const a = props.activity
const middle = (a?.stage_minutes ?? []).slice(0, -1)
const form = reactive({
  type: a?.type ?? 'marathon',
  name: a?.name ?? '',
  start_date: a?.start_date ?? '',
  end_date: a?.end_date ?? '',
  url: a?.url ?? '',
  color: a?.color ?? '#7A8C5C',
  goal_minutes: a?.goal_minutes ?? '',
  current_minutes: a?.current_minutes ?? 0,
  stage1: middle[0] ?? '',
  stage2: middle[1] ?? ''
})
const saving = ref(false)

function payload () {
  const { stage1, stage2, ...body } = form
  if (form.type !== 'marathon') return { ...body, goal_minutes: null, current_minutes: null, stage_minutes: null }
  return { ...body, current_minutes: form.current_minutes || 0, stage_minutes: [stage1, stage2, form.goal_minutes].filter(s => s !== '') }
}

async function save () {
  saving.value = true
  try {
    if (a) await http.put(`/activities/${a.id}`, payload())
    else await http.post('/activities', payload())
    toast('已儲存')
    emit('saved')
  } finally {
    saving.value = false
  }
}

async function remove () {
  if (!window.confirm(`確定刪除「${a.name}」？刪除後無法復原。`)) return
  saving.value = true
  try {
    await http.delete(`/activities/${a.id}`)
    toast('已刪除')
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>
