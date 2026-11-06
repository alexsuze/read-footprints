<template>
  <h1 class="h3">設定</h1>
  <ul class="nav nav-tabs mb-3">
    <li v-for="t in tabs" :key="t.key" class="nav-item">
      <a href="#" class="nav-link" :class="{ active: tab === t.key }" @click.prevent="tab = t.key">
        {{ t.label }}<span v-if="t.key === 'users' && auth.pendingCount" class="badge rounded-pill text-bg-danger ms-2">{{ auth.pendingCount }}</span>
      </a>
    </li>
  </ul>

  <CategoryManager v-if="tab === 'categories'" />

  <UserApproval v-else-if="tab === 'users'" />

  <template v-else-if="tab === 'profile'">
    <div class="card mb-3" style="max-width: 480px">
      <div class="card-body d-flex align-items-center gap-3">
        <img v-if="auth.user?.picture" :src="auth.user.picture" alt="" class="rounded-circle" width="56" height="56" referrerpolicy="no-referrer">
        <div>
          <div class="fw-medium">{{ auth.user?.name }}</div>
          <div class="text-secondary small">{{ auth.user?.email }}</div>
          <span class="pill pill-active mt-1">{{ auth.isAdmin ? '管理員' : '一般使用者' }}</span>
        </div>
      </div>
    </div>
    <LegacyImport v-if="auth.isAdmin" />
  </template>

  <LoanManager v-else-if="tab === 'loans'" />
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import CategoryManager from '../components/CategoryManager.vue'
import LegacyImport from '../components/LegacyImport.vue'
import LoanManager from '../components/LoanManager.vue'
import UserApproval from '../components/UserApproval.vue'

const auth = useAuthStore()
const route = useRoute()
const tabs = computed(() => [
  { key: 'categories', label: '分類管理' },
  ...(auth.isAdmin ? [{ key: 'users', label: '使用者核准' }, { key: 'loans', label: '租借管理' }] : []),
  { key: 'profile', label: '個人資料' }
])
// 信裡的連結會帶 ?tab=users 直接開到使用者核准
const tab = ref(tabs.value.some(t => t.key === route.query.tab) ? route.query.tab : 'categories')
watch(() => route.query.tab, t => { if (tabs.value.some(x => x.key === t)) tab.value = t })
</script>
