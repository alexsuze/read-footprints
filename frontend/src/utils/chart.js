// 圖表共用設定：日式書房色系
export const CHART_COLORS = ['#8B6B4A', '#7A8C5C', '#5B6B7A', '#B5493A', '#C9A227', '#6F8F8A', '#A07A9A', '#B8865B', '#4F6D5A', '#9AA3AB']
const INK = '#6F6A60'
const LINE = '#E2DACA'

export const money = v => `$${Math.round(v).toLocaleString('zh-TW')}`

// 長條圖：labels 為 x 軸，values 為數值；avg 有值時畫成虛線平均線
// interval：x 軸標籤間隔（0 = 全部顯示，或傳函式決定哪些顯示）
export function barOption ({ labels, values, color = CHART_COLORS[0], format = v => v, rotate = 0, avg = null, avgLabel = '', interval = 0 }) {
  return {
    color: [color],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: v => (v == null ? '—' : format(v)) },
    grid: { left: 8, right: 16, top: 28, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: labels, axisLabel: { color: INK, rotate, interval }, axisLine: { lineStyle: { color: LINE } }, axisTick: { show: false } },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { color: INK }, splitLine: { lineStyle: { color: LINE } } },
    series: [{
      type: 'bar',
      data: values,
      barMaxWidth: 36,
      itemStyle: { borderRadius: [3, 3, 0, 0] },
      markLine: avg == null
        ? undefined
        : {
            silent: true,
            symbol: 'none',
            z: 10, // 畫在長條上面，文字才不會被擋住
            lineStyle: { color: '#B5493A', type: 'dashed' },
            label: { formatter: `${avgLabel} ${format(avg)}`, position: 'insideEndTop', color: '#B5493A', backgroundColor: 'rgba(251, 248, 241, .9)', padding: [1, 4], borderRadius: 3 },
            data: [{ yAxis: avg }]
          }
    }]
  }
}

// 甜甜圈圖：前 10 名顯示標籤；compact（手機）縮小圓、標籤換行並只標前 6 名
export function pieOption (items, compact = false) {
  return {
    color: CHART_COLORS,
    tooltip: { trigger: 'item', formatter: '{b}：{c} 本（{d}%）' },
    series: [{
      type: 'pie',
      radius: compact ? ['30%', '52%'] : ['40%', '68%'],
      data: items.map((item, i) => ({
        name: item.name,
        value: item.count,
        label: { show: i < (compact ? 6 : 10), formatter: compact ? '{b}\n{d}%' : '{b} {d}%', color: INK, fontSize: compact ? 11 : 12 },
        labelLine: { show: i < (compact ? 6 : 10), length: compact ? 8 : 15, length2: compact ? 6 : 10 }
      }))
    }]
  }
}
