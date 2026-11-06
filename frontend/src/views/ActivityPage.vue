<template>
  <div class="d-flex flex-wrap align-items-center gap-2 gap-md-3 mb-3">
    <h1 class="h3 mb-0">活動紀錄</h1>
    <span class="text-secondary small me-auto me-md-0">進行中 {{ counts.ongoing }} · 已結束 {{ counts.ended }}</span>
    <div class="btn-group ms-md-auto" role="group" aria-label="檢視方式">
      <button v-for="v in VIEWS" :key="v.value" type="button" class="btn btn-outline-primary text-nowrap" :class="{ active: view === v.value }" :aria-label="v.label" :title="v.label" @click="setView(v.value)"><i class="bi" :class="v.icon"></i><span class="d-none d-sm-inline ms-1">{{ v.label }}</span></button>
    </div>
    <div class="btn-group" role="group" aria-label="活動種類">
      <button v-for="f in FILTERS" :key="f.value" type="button" class="btn btn-outline-primary text-nowrap" :class="{ active: filter === f.value }" @click="filter = f.value">{{ f.label }}</button>
    </div>
    <button type="button" class="btn btn-primary text-nowrap" @click="openForm(null)"><i class="bi bi-plus-lg me-1"></i>新增活動</button>
  </div>

  <section v-if="view === 'calendar'" class="card"><div class="card-body"><ActivityCalendar :activities="shown" /></div></section>

  <template v-else>
    <div class="form-check form-switch mb-3">
      <input id="show-ended" v-model="showEnded" class="form-check-input" type="checkbox" role="switch">
      <label class="form-check-label small" for="show-ended">顯示已結束的活動</label>
    </div>

    <p v-if="loaded && !listed.length" class="text-secondary text-center py-5">{{ emptyText }}</p>

    <section
      v-for="a in pageItems"
      :key="a.id"
      class="card mb-3 activity"
      :class="{ ended: status(a).key === 'ended' }"
      :style="{ borderLeftColor: status(a).key === 'ended' ? 'var(--rf-line-2)' : a.color }"
    >
      <div class="card-body">
        <div class="d-flex flex-wrap align-items-center gap-2" :class="{ 'mb-3': a.type === 'marathon' }">
          <span class="pill" :class="a.type === 'marathon' ? 'pill-active' : 'pill-quiz'">{{ TYPE_LABELS[a.type] }}</span>
          <h2 class="h5 mb-0">{{ a.name }}</h2>
          <span class="pill" :class="`pill-${status(a).key}`">{{ status(a).label }}</span>
          <span class="text-secondary small ms-md-auto">
            {{ a.start_date }} ～ {{ a.end_date }}
            <template v-if="a.url"> · <a :href="a.url" target="_blank" rel="noopener noreferrer">活動網址</a></template>
          </span>
          <button type="button" class="btn btn-sm btn-outline-primary" @click="openForm(a)">編輯</button>
        </div>
        <MarathonProgress v-if="a.type === 'marathon'" :activity="a" :muted="status(a).key === 'ended'">
          <AddMinutes v-if="status(a).key !== 'ended'" :activity="a" />
        </MarathonProgress>
      </div>
    </section>

    <PageNav v-if="pages > 1" :page="page" :pages="pages" @go="page = $event" />
  </template>

  <ActivityForm v-if="formOpen" :activity="editing" @close="formOpen = false" @saved="onSaved" />
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import http from '../api/http'
import ActivityCalendar from '../components/ActivityCalendar.vue'
import ActivityForm from '../components/ActivityForm.vue'
import AddMinutes from '../components/AddMinutes.vue'
import MarathonProgress from '../components/MarathonProgress.vue'
import PageNav from '../components/PageNav.vue'
import { useToday } from '../composables/useToday'
import { activityStatus, TYPE_LABELS } from '../utils/activity'

const FILTERS = [{ value: 'all', label: '全部' }, { value: 'marathon', label: '馬拉松' }, { value: 'quiz', label: '問答' }]
const VIEWS = [{ value: 'list', label: '列表', icon: 'bi-list-ul' }, { value: 'calendar', label: '月曆', icon: 'bi-calendar3' }]
const VIEW_KEY = 'rf.activityView'
const ENDED_KEY = 'rf.activityShowEnded'
const PER_PAGE = 5

const activities = ref([])
const loaded = ref(false)
const filter = ref('all')
const formOpen = ref(false)
const editing = ref(null)
const view = ref(readView())
const showEnded = ref(readFlag(ENDED_KEY))
const page = ref(1)

const now = useToday()
const status = a => activityStatus(a, now.value)
const shown = computed(() => activities.value.filter(a => filter.value === 'all' || a.type === filter.value))
// 列表檢視：預設不顯示已結束，每頁 5 個
const listed = computed(() => shown.value.filter(a => showEnded.value || status(a).key !== 'ended'))
const pages = computed(() => Math.max(1, Math.ceil(listed.value.length / PER_PAGE)))
const pageItems = computed(() => listed.value.slice((page.value - 1) * PER_PAGE, page.value * PER_PAGE))
const emptyText = computed(() => {
  if (!activities.value.length) return '還沒有活動，按「新增活動」記下第一個閱讀活動'
  if (!shown.value.length) return '沒有這個種類的活動'
  return '沒有進行中或未開始的活動，打開「顯示已結束的活動」可以看舊的活動'
})
const counts = computed(() => ({
  ongoing: activities.value.filter(a => status(a).key === 'ongoing').length,
  ended: activities.value.filter(a => status(a).key === 'ended').length
}))

async function load () {
  activities.value = (await http.get('/activities')).data
  loaded.value = true
}

watch([filter, showEnded], () => { page.value = 1 })
watch(showEnded, v => { try { localStorage.setItem(ENDED_KEY, v ? '1' : '0') } catch { /* 無法儲存就算了 */ } })
watch(pages, p => { if (page.value > p) page.value = p })

function readFlag (key) {
  try { return localStorage.getItem(key) === '1' } catch { return false }
}

function openForm (activity) {
  editing.value = activity
  formOpen.value = true
}

function onSaved () {
  formOpen.value = false
  load()
}

// 記住上次的檢視方式，只存在這台瀏覽器
function readView () {
  try { return localStorage.getItem(VIEW_KEY) === 'calendar' ? 'calendar' : 'list' } catch { return 'list' }
}

function setView (value) {
  view.value = value
  try { localStorage.setItem(VIEW_KEY, value) } catch { /* 無法儲存就算了 */ }
}

onMounted(load)
</script>

<style scoped>
.activity { border-left-width: 4px; }
.activity.ended { opacity: .75; }
.pill-ongoing { background: #E1E7D5; color: #5F6F43; }
.pill-upcoming { background: #F1E3C8; color: var(--rf-wood); }
.pill-ended { background: var(--rf-paper-2); color: var(--rf-ink-2); }
.pill-quiz { background: #DDE2E7; color: #3F4C58; }
</style>
