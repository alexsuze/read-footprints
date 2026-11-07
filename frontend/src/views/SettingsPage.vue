<template>
  <h1 class="h3">設定</h1>
  <ul class="nav nav-tabs mb-3 tabs-scroll">
    <li v-for="t in tabs" :key="t.key" class="nav-item">
      <a href="#" class="nav-link" :class="{ active: tab === t.key }" @click.prevent="tab = t.key">
        {{ t.label }}<span v-if="t.key === 'users' && auth.pendingCount" class="badge rounded-pill text-bg-danger ms-2">{{ auth.pendingCount }}</span>
      </a>
    </li>
  </ul>

  <CategoryManager v-if="tab === 'categories'" />

  <ColumnSettings v-else-if="tab === 'columns'" />

  <UserApproval v-else-if="tab === 'users'" />

  <ProfileCard v-else-if="tab === 'profile'" />

  <LoanManager v-else-if="tab === 'loans'" />
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import CategoryManager from '../components/CategoryManager.vue'
import ColumnSettings from '../components/ColumnSettings.vue'
import LoanManager from '../components/LoanManager.vue'
import ProfileCard from '../components/ProfileCard.vue'
import UserApproval from '../components/UserApproval.vue'

const auth = useAuthStore()
const route = useRoute()
const tabs = computed(() => [
  { key: 'categories', label: '分類管理' },
  { key: 'columns', label: '報表欄位' },
  ...(auth.isAdmin ? [{ key: 'users', label: '使用者核准' }, { key: 'loans', label: '租借管理' }] : []),
  { key: 'profile', label: '個人資料' }
])
// 信裡的連結會帶 ?tab=users 直接開到使用者核准
const tab = ref(tabs.value.some(t => t.key === route.query.tab) ? route.query.tab : 'categories')
watch(() => route.query.tab, t => { if (tabs.value.some(x => x.key === t)) tab.value = t })
// 手機上分頁列可左右捲，把目前分頁捲進畫面
watch(tab, () => nextTick(() => document.querySelector('.tabs-scroll .active')?.scrollIntoView({ block: 'nearest', inline: 'nearest' })), { immediate: true })
</script>
