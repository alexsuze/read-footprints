// 閱讀統計的計算（規劃書 4.5），公式照 1.0 統計頁搬。日期一律 yyyy-MM-dd 字串

// 1.0「總閱讀時數」格式：X天 Y時 Z分（一天 = 1440 分）
export function formatMinutes (total) {
  if (!total) return '0分'
  const parts = []
  const days = Math.floor(total / 1440)
  const hours = Math.floor((total % 1440) / 60)
  const minutes = total % 60
  if (days) parts.push(`${days}天`)
  if (hours) parts.push(`${hours}時`)
  if (minutes) parts.push(`${minutes}分`)
  return parts.join(' ')
}

export const RANKING_TYPES = [
  { value: 'category', label: '分類', icon: 'bi-bookmark' },
  { value: 'publisher', label: '出版社', icon: 'bi-building' },
  { value: 'author', label: '作者', icon: 'bi-person' },
  { value: 'translator', label: '譯者', icon: 'bi-translate' }
]

// 排行：依本數由多到少，占比 = 本數 / 所有項目本數合計（作者、譯者一本可算多人）
export function ranking (books, type) {
  const counts = new Map()
  const add = name => counts.set(name, (counts.get(name) || 0) + 1)
  for (const b of books) {
    if (type === 'category') add(b.category_name || '未分類')
    else if (type === 'publisher') add(b.publisher || '未知出版社')
    else {
      for (const name of (type === 'author' ? b.authors : b.translators) || []) {
        if (name && name.trim()) add(name)
      }
    }
  }
  const total = [...counts.values()].reduce((s, c) => s + c, 0)
  return [...counts]
    .map(([name, count]) => ({ name, count, percentage: total ? count * 100 / total : 0 }))
    .sort((a, b) => b.count - a.count)
}

// 購買力 KPI：年數 = 今年 - 最早購買年 + 1；月均 = 年均 / 12；日均 = 月均 / 30
export function spending (books, currentYear) {
  const total = books.reduce((s, b) => s + (b.price || 0), 0)
  const priced = books.filter(b => b.price > 0)
  const years = priced.filter(b => b.buy_date).map(b => Number(b.buy_date.slice(0, 4))).filter(Boolean)
  const span = years.length ? Math.max(1, currentYear - Math.min(...years) + 1) : 1
  const yearly = total / span
  return {
    total,
    average: priced.length ? total / priced.length : 0,
    yearly,
    monthly: yearly / 12,
    daily: yearly / 12 / 30
  }
}

const periodOf = (date, mode) => date.slice(0, mode === 'year' ? 4 : 7)

// 依年或月加總，舊到新，只列有資料的期間
function groupBy (items, mode, dateOf, valueOf) {
  const map = new Map()
  for (const item of items) {
    const date = dateOf(item)
    if (!date) continue
    const key = periodOf(date, mode)
    map.set(key, (map.get(key) || 0) + valueOf(item))
  }
  return [...map].map(([period, value]) => ({ period, value })).sort((a, b) => a.period.localeCompare(b.period))
}

export const spendingByPeriod = (books, mode) =>
  groupBy(books.filter(b => b.price > 0), mode, b => b.buy_date, b => b.price)

export const completedByPeriod = (books, mode) =>
  groupBy(books.filter(b => b.read), mode, b => b.read_date, () => 1)

export const minutesByYear = logs => groupBy(logs, 'year', l => l.date, l => l.minutes)

// 某一年 12 個月的分鐘數（沒紀錄的月份為 0）
export function minutesByMonth (logs, year) {
  const months = Array.from({ length: 12 }, (_, i) => ({ period: `${year}-${String(i + 1).padStart(2, '0')}`, value: 0 }))
  for (const l of logs) {
    if (l.date.startsWith(`${year}-`)) months[Number(l.date.slice(5, 7)) - 1].value += l.minutes
  }
  return months
}

// 連續閱讀里程碑（1.0 首頁）：無 0、鐵 7、銅 30、銀 180、金 365、白金 750、鑽石 1500 天
export const MILESTONES = [
  { name: '無', days: 0, color: '#C9BFA9', icon: 'bi-book' },
  { name: '鐵', days: 7, color: '#6F6A60', icon: 'bi-shield' },
  { name: '銅', days: 30, color: '#B07A45', icon: 'bi-award' },
  { name: '銀', days: 180, color: '#9AA3AB', icon: 'bi-award-fill' },
  { name: '金', days: 365, color: '#C9A227', icon: 'bi-trophy' },
  { name: '白金', days: 750, color: '#A8B3B8', icon: 'bi-trophy-fill' },
  { name: '鑽石', days: 1500, color: '#6FA8C0', icon: 'bi-gem' }
]

// 目前等級、下一級與往下一級的進度（%，四捨五入；已達最高為 100）
export function milestone (days) {
  const index = MILESTONES.findLastIndex(m => days >= m.days)
  const current = MILESTONES[index]
  const next = MILESTONES[index + 1] || null
  const progress = next ? Math.round((days - current.days) * 100 / (next.days - current.days)) : 100
  return { current, next, progress, index }
}
