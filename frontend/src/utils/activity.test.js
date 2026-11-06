import assert from 'node:assert/strict'
import { test } from 'node:test'
import { activityStatus, monthWeeks, weekBars } from './activity.js'

const a = (name, start_date, end_date) => ({ name, start_date, end_date })

test('活動狀態與剩餘天數', () => {
  const x = a('x', '2026-11-02', '2026-11-30')
  assert.deepEqual(activityStatus(x, '2026-11-01'), { key: 'upcoming', label: '未開始 · 11/2 起' })
  assert.deepEqual(activityStatus(x, '2026-11-04'), { key: 'ongoing', label: '進行中 · 剩 26 天' })
  assert.equal(activityStatus(x, '2026-11-30').key, 'ongoing')
  assert.equal(activityStatus(x, '2026-12-01').key, 'ended')
})

test('月曆從週日開始並補齊前後月', () => {
  const weeks = monthWeeks(2026, 10) // 2026 年 11 月，1 號是週日
  assert.equal(weeks.length, 5)
  assert.equal(weeks[0][0], '2026-11-01')
  assert.equal(weeks[4][6], '2026-12-05')
  const feb = monthWeeks(2026, 1) // 2026 年 2 月，剛好四週
  assert.equal(feb.length, 4)
  const mar = monthWeeks(2026, 7) // 2026 年 8 月，1 號週六，需要六週
  assert.equal(mar.length, 6)
  assert.equal(mar[0][0], '2026-07-26')
})

test('色條跨週裁切，重疊時分 lane，不重疊共用 lane', () => {
  const week = monthWeeks(2026, 10)[1] // 11/8 到 11/14
  const bars = weekBars(week, [
    a('長', '2026-11-02', '2026-11-30'),
    a('早', '2026-11-08', '2026-11-09'),
    a('晚', '2026-11-11', '2026-11-12'),
    a('週外', '2026-11-20', '2026-11-21')
  ])
  assert.equal(bars.length, 3)
  const [long, early, late] = bars
  assert.deepEqual([long.from, long.to, long.lane, long.starts, long.ends], [0, 6, 0, false, false])
  assert.deepEqual([early.from, early.to, early.lane, early.starts, early.ends], [0, 1, 1, true, true])
  assert.deepEqual([late.from, late.to, late.lane], [3, 4, 1])
})
