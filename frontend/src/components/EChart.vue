<template>
  <div class="chart-scroll">
    <div ref="el" :style="{ height: `${height}px`, minWidth: `${minWidth}px` }"></div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { init, use } from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, MarkLineComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// ECharts 外框：跟著容器寬度縮放；資料太多時設 minWidth 讓圖表可以左右捲動
use([BarChart, PieChart, GridComponent, MarkLineComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: Number, default: 320 },
  minWidth: { type: Number, default: 0 }
})

const el = ref(null)
let chart
let observer

onMounted(() => {
  chart = init(el.value)
  chart.setOption(props.option)
  observer = new ResizeObserver(() => chart.resize())
  observer.observe(el.value)
})

watch(() => props.option, option => chart?.setOption(option, true))

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
})
</script>

<style scoped>
.chart-scroll { overflow-x: auto; }
</style>
