<template>
  <div class="card" style="max-width: 560px">
    <div class="card-body">
      <form class="d-flex gap-2 mb-3" @submit.prevent="add">
        <input v-model="newName" class="form-control" placeholder="新分類名稱" aria-label="新分類名稱" maxlength="30" required>
        <button type="submit" class="btn btn-primary text-nowrap">新增</button>
      </form>
      <ul class="list-group list-group-flush">
        <li v-for="c in categories" :key="c.id" class="list-group-item bg-transparent d-flex align-items-center gap-2 px-0">
          <template v-if="editing === c.id">
            <input v-model="editName" class="form-control form-control-sm" aria-label="分類名稱" maxlength="30" @keyup.enter="rename(c)">
            <button type="button" class="btn btn-sm btn-primary text-nowrap" @click="rename(c)">儲存</button>
            <button type="button" class="btn btn-sm btn-outline-primary text-nowrap" @click="editing = null">取消</button>
          </template>
          <template v-else>
            <span class="me-auto">{{ c.name }}</span>
            <button type="button" class="btn btn-sm btn-outline-primary text-nowrap" @click="startEdit(c)">改名</button>
            <button type="button" class="btn btn-sm btn-outline-danger text-nowrap" @click="remove(c)">刪除</button>
          </template>
        </li>
        <li v-if="loaded && !categories.length" class="list-group-item bg-transparent text-secondary px-0">還沒有分類</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import http from '../api/http'
import { toast } from './AppToast.vue'

// 分類管理（規劃書 4.6）
const categories = ref([])
const loaded = ref(false)
const newName = ref('')
const editing = ref(null)
const editName = ref('')

async function load () {
  categories.value = (await http.get('/categories')).data
  loaded.value = true
}

async function add () {
  await http.post('/categories', { name: newName.value })
  newName.value = ''
  await load()
}

function startEdit (c) {
  editing.value = c.id
  editName.value = c.name
}

async function rename (c) {
  await http.put(`/categories/${c.id}`, { name: editName.value })
  editing.value = null
  await load()
}

async function remove (c) {
  if (!window.confirm(`確定刪除「${c.name}」？這個分類的書會變成未分類。`)) return
  await http.delete(`/categories/${c.id}`)
  toast('已刪除')
  await load()
}

onMounted(load)
</script>
