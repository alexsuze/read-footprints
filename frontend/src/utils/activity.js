// 活動狀態與月曆排版（規劃書 4.2、4.4）。日期一律 yyyy-MM-dd 字串，可直接比大小

export const TYPE_LABELS = { marathon: '馬拉松', quiz: '問答' }

export const today = () => new Date().toLocaleDateString('sv')

const DAY_MS = 86400000
const shortDate = iso => `${Number(iso.slice(5, 7))}/${Number(iso.slice(8, 10))}`

export const daysLeft = (a, now) => (Date.parse(a.end_date) - Date.parse(now)) / DAY_MS

export function activityStatus (a, now) {
  if (now < a.start_date) return { key: 'upcoming', label: `未開始 · ${shortDate(a.start_date)} 起` }
  if (now > a.end_date) return { key: 'ended', label: '已結束' }
  return { key: 'ongoing', label: `進行中 · 剩 ${daysLeft(a, now)} 天` }
}

// 月曆每週 7 個日期，從週日開始，含前後月補齊
export function monthWeeks (year, month) {
  const offset = new Date(year, month, 1).getDay()
  const days = new Date(year, month + 1, 0).getDate()
  const weeks = []
  for (let w = 0; w * 7 < offset + days; w++) {
    weeks.push(Array.from({ length: 7 }, (_, i) => new Date(year, month, 1 - offset + w * 7 + i).toLocaleDateString('sv')))
  }
  return weeks
}

// 一週內的活動色條：from / to 是星期欄位，重疊的活動排到不同 lane
export function weekBars (week, activities) {
  const inWeek = activities
    .filter(a => a.start_date <= week[6] && a.end_date >= week[0])
    .sort((a, b) => a.start_date.localeCompare(b.start_date) || b.end_date.localeCompare(a.end_date))
  const laneEnds = []
  return inWeek.map(activity => {
    const from = activity.start_date < week[0] ? 0 : week.indexOf(activity.start_date)
    const to = activity.end_date > week[6] ? 6 : week.indexOf(activity.end_date)
    let lane = laneEnds.findIndex(end => end < from)
    if (lane === -1) lane = laneEnds.length
    laneEnds[lane] = to
    return { activity, from, to, lane, starts: activity.start_date >= week[0], ends: activity.end_date <= week[6] }
  })
}
