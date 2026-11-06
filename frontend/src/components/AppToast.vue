<script>
import { reactive } from 'vue'

// 全站共用的提示訊息：toast('已儲存', 'success')。type 對應 Bootstrap 顏色：success / danger / warning / info
const items = reactive([])
let seq = 0

export function toast (message, type = 'success') {
  const id = ++seq
  items.push({ id, message, type })
  setTimeout(() => items.splice(items.findIndex(i => i.id === id), 1), 3000)
}
</script>

<script setup>
const list = items
</script>

<template>
  <div class="toast-container position-fixed top-0 end-0 p-3" style="z-index: 1080">
    <div v-for="t in list" :key="t.id" class="toast show align-items-center border-0" :class="`text-bg-${t.type}`" role="status">
      <div class="d-flex">
        <div class="toast-body">{{ t.message }}</div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" aria-label="關閉" @click="list.splice(list.indexOf(t), 1)"></button>
      </div>
    </div>
  </div>
</template>
