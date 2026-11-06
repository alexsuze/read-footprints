<template>
  <div class="card">
    <div class="table-responsive">
      <table class="table align-middle mb-0">
        <thead><tr><th></th><th>名稱</th><th>Email</th><th>狀態</th><th>申請時間</th><th>最後登入</th><th></th></tr></thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td><img v-if="u.picture" :src="u.picture" alt="" class="rounded-circle" width="32" height="32" referrerpolicy="no-referrer"></td>
            <td>{{ u.name }} <span v-if="u.admin" class="text-secondary small">（管理員）</span></td>
            <td class="text-secondary">{{ u.email }}</td>
            <td><span class="pill" :class="`pill-${u.status}`">{{ STATUS[u.status] }}</span></td>
            <td class="text-secondary small">{{ fmt(u.created_at) }}</td>
            <td class="text-secondary small">{{ fmt(u.last_login_at) }}</td>
            <td class="text-end text-nowrap">
              <template v-if="u.id !== auth.user?.id">
                <button v-if="u.status !== 'active'" class="btn btn-sm btn-primary me-1" @click="setStatus(u, 'active')">{{ u.status === 'pending' ? '核准' : '重新啟用' }}</button>
                <button v-if="u.status !== 'disabled'" class="btn btn-sm btn-outline-danger" @click="setStatus(u, 'disabled')">停用</button>
              </template>
            </td>
          </tr>
          <tr v-if="loaded && !users.length"><td colspan="7" class="text-center text-secondary py-4">還沒有其他使用者</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import http from '../api/http'
import { toast } from './AppToast.vue'
import { useAuthStore } from '../stores/auth'

const STATUS = { pending: '待核准', active: '使用中', disabled: '已停用' }
const auth = useAuthStore()
const users = ref([])
const loaded = ref(false)

const fmt = iso => iso ? new Date(iso).toLocaleString('zh-TW', { hour12: false }).slice(0, 16) : '—'

async function load () {
  const { data } = await http.get('/admin/users')
  users.value = data
  loaded.value = true
  auth.pendingCount = data.filter(u => u.status === 'pending').length
}

async function setStatus (u, status) {
  const { data } = await http.put(`/admin/users/${u.id}/status`, { status })
  Object.assign(u, data)
  auth.pendingCount = users.value.filter(x => x.status === 'pending').length
  toast(`${u.name} 已${STATUS[status]}`)
}

onMounted(load)
</script>
