<template>
  <div class="d-flex flex-wrap align-items-center gap-3 mb-3">
    <h1 class="h3 mb-0">活動紀錄</h1>
    <span class="text-secondary small">進行中 {{ counts.ongoing }} · 已結束 {{ counts.ended }}</span>
    <div class="btn-group ms-auto" role="group" aria-label="檢視方式">
      <button v-for="v in VIEWS" :key="v.value" type="button" class="btn btn-outline-primary text-nowrap" :class="{ active: view === v.value }" @click="setView(v.value)"><i class="bi me-1" :class="v.icon"></i>{{ v.label }}</button>
    </div>
    <div class="btn-group" role="group" aria-label="活動種類">
      <button v-for="f in FILTERS" :key="f.value" type="button" class="btn btn-outline-primary text-nowrap" :class="{ active: filter === f.value }" @click="filter = f.value">{{ f.label }}</button>
    </div>
    <button type="button" class="btn btn-primary text-nowrap" @click="openForm(null)"><i class="bi bi-plus-lg me-1"></i>新增活動</button>
  </div>

  <section v-if="view === 'calendar'" class="card"><div class="card-body"><ActivityCalendar :activities="shown" /></div></section>

  <p v-else-if="loaded && !shown.length" class="text-secondary text-center py-5">還沒有活動</p>

  <template v-else>
    <section
      v-for="a in shown"
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
  </template>

  <ActivityForm v-if="formOpen" :activity="editing" @close="formOpen = false" @saved="onSaved" />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../api/http'
import ActivityCalendar from '../components/ActivityCalendar.vue'
import ActivityForm from '../components/ActivityForm.vue'
import AddMinutes from '../components/AddMinutes.vue'
import MarathonProgress from '../components/MarathonProgress.vue'
import { useToday } from '../composables/useToday'
import { activityStatus, TYPE_LABELS } from '../utils/activity'

const FILTERS = [{ value: 'all', label: '全部' }, { value: 'marathon', label: '馬拉松' }, { value: 'quiz', label: '問答' }]
const VIEWS = [{ value: 'list', label: '列表', icon: 'bi-list-ul' }, { value: 'calendar', label: '月曆', icon: 'bi-calendar3' }]
const VIEW_KEY = 'rf.activityView'

const activities = ref([])
const loaded = ref(false)
const filter = ref('all')
const formOpen = ref(false)
const editing = ref(null)
const view = ref(readView())

const now = useToday()
const status = a => activityStatus(a, now.value)
const shown = computed(() => activities.value.filter(a => filter.value === 'all' || a.type === filter.value))
const counts = computed(() => ({
  ongoing: activities.value.filter(a => status(a).key === 'ongoing').length,
  ended: activities.value.filter(a => status(a).key === 'ended').length
}))

async function load () {
  activities.value = (await http.get('/activities')).data
  loaded.value = true
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
