<template>
  <div class="row g-3">
    <div class="col-lg-7">
      <section class="card h-100">
        <div class="card-body">
          <h2 class="h6 mb-2">{{ label }}分布<span v-if="type !== 'category' && items.length > BAR_LIMIT" class="text-secondary small fw-normal ms-2">前 {{ BAR_LIMIT }} 名</span></h2>
          <p v-if="!items.length" class="text-secondary text-center py-5 mb-0">沒有資料</p>
          <EChart v-else :option="option" :height="340" :min-width="type === 'category' ? 0 : Math.min(items.length, BAR_LIMIT) * 44" />
        </div>
      </section>
    </div>
    <div class="col-lg-5">
      <section class="card h-100">
        <div class="card-body">
          <h2 class="h6 mb-2">{{ label }}排行<span class="text-secondary small fw-normal ms-2">共 {{ items.length }} 項</span></h2>
          <div class="table-wrap">
            <table class="table table-sm mb-0">
              <thead><tr><th class="rank">名次</th><th>名稱</th><th class="text-end">本數</th><th class="text-end">占比</th></tr></thead>
              <tbody>
                <tr v-for="r in ranks" :key="r.name">
                  <td class="rank"><span class="medal" :class="`m${r.rank}`">{{ r.rank }}</span></td>
                  <td class="name">{{ r.name }}</td>
                  <td class="text-end">{{ r.count }}</td>
                  <td class="text-end text-secondary">{{ r.percentage.toFixed(1) }}%</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import EChart from '../EChart.vue'
import { barOption, pieOption } from '../../utils/chart'
import { ranking, RANKING_TYPES } from '../../utils/stats'

// 分類 / 出版社 / 作者 / 譯者排行（購買分析、完讀分析共用，同 1.0）：分類用甜甜圈圖，其他用長條圖
const props = defineProps({
  books: { type: Array, required: true },
  type: { type: String, required: true },
  color: { type: String, default: undefined }
})

const BAR_LIMIT = 30
const compact = window.matchMedia('(max-width: 575.98px)').matches
const label = computed(() => RANKING_TYPES.find(t => t.value === props.type).label)
const items = computed(() => ranking(props.books, props.type))
// 同本數同名次
const ranks = computed(() => {
  let rank = 0
  return items.value.map((r, i, all) => {
    if (i === 0 || all[i - 1].count !== r.count) rank = i + 1
    return { ...r, rank }
  })
})
const option = computed(() => {
  if (props.type === 'category') return pieOption(items.value, compact)
  const top = items.value.slice(0, BAR_LIMIT)
  return barOption({ labels: top.map(r => r.name), values: top.map(r => r.count), color: props.color, format: v => `${v} 本`, rotate: 45 })
})
</script>

<style scoped>
.table-wrap { max-height: 340px; overflow-y: auto; }
.table-wrap thead th { position: sticky; top: 0; background: var(--rf-card); }
.rank { width: 52px; }
.name { word-break: break-all; }
.medal { display: inline-block; min-width: 24px; text-align: center; border-radius: 12px; font-size: 12px; color: var(--rf-ink-2); }
.medal.m1 { background: #E9D9A6; color: #6E5338; }
.medal.m2 { background: #E1E3E4; color: #4F5458; }
.medal.m3 { background: #E6CBB0; color: #6E4A2C; }
</style>
