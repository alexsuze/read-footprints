import { test } from 'node:test'
import assert from 'node:assert/strict'
import { completedByPeriod, formatMinutes, milestone, minutesByMonth, minutesByYear, ranking, spending, spendingByPeriod } from './stats.js'

const books = [
  { category_name: '小說', publisher: '皇冠', authors: ['東野圭吾'], translators: ['王蘊潔'], price: 300, buy_date: '2024-03-01', read: true, read_date: '2024-05-02' },
  { category_name: '小說', publisher: '皇冠', authors: ['東野圭吾', ' '], translators: [], price: 200, buy_date: '2026-01-10', read: true, read_date: '2026-02-01' },
  { category_name: null, publisher: '', authors: ['村上春樹'], translators: ['賴明珠'], price: 0, buy_date: '2026-01-20', read: false, read_date: null },
  { category_name: '散文', publisher: '遠流', authors: [], translators: [], price: null, buy_date: null, read: true, read_date: '2026-02-15' }
]

test('formatMinutes 照 1.0 格式', () => {
  assert.equal(formatMinutes(0), '0分')
  assert.equal(formatMinutes(45), '45分')
  assert.equal(formatMinutes(1440 + 60 + 5), '1天 1時 5分')
  assert.equal(formatMinutes(120), '2時')
})

test('ranking 依本數排序，空白歸到未分類 / 未知出版社', () => {
  assert.deepEqual(ranking(books, 'category').map(r => [r.name, r.count]), [['小說', 2], ['未分類', 1], ['散文', 1]])
  assert.deepEqual(ranking(books, 'publisher').map(r => [r.name, r.count]), [['皇冠', 2], ['未知出版社', 1], ['遠流', 1]])
  const authors = ranking(books, 'author')
  assert.deepEqual(authors.map(r => [r.name, r.count]), [['東野圭吾', 2], ['村上春樹', 1]])
  assert.ok(Math.abs(authors[0].percentage - 200 / 3) < 1e-9)
  assert.equal(ranking(books, 'translator').length, 2)
})

test('spending 年數從最早購買年算到今年', () => {
  const s = spending(books, 2026)
  assert.equal(s.total, 500)
  assert.equal(s.average, 250)
  assert.ok(Math.abs(s.yearly - 500 / 3) < 1e-9)
  assert.ok(Math.abs(s.daily - 500 / 3 / 12 / 30) < 1e-9)
  assert.deepEqual(spending([], 2026), { total: 0, average: 0, yearly: 0, monthly: 0, daily: 0 })
})

test('依年 / 月加總', () => {
  assert.deepEqual(spendingByPeriod(books, 'year'), [{ period: '2024', value: 300 }, { period: '2026', value: 200 }])
  assert.deepEqual(completedByPeriod(books, 'month'), [{ period: '2024-05', value: 1 }, { period: '2026-02', value: 2 }])
  const logs = [{ date: '2025-12-31', minutes: 30 }, { date: '2026-01-01', minutes: 20 }, { date: '2026-01-15', minutes: 10 }]
  assert.deepEqual(minutesByYear(logs), [{ period: '2025', value: 30 }, { period: '2026', value: 30 }])
  const months = minutesByMonth(logs, 2026)
  assert.equal(months.length, 12)
  assert.deepEqual(months[0], { period: '2026-01', value: 30 })
  assert.equal(months[11].value, 0)
})

test('milestone 等級與進度', () => {
  assert.equal(milestone(0).current.name, '無')
  assert.equal(milestone(6).next.name, '鐵')
  assert.equal(milestone(6).progress, 86)
  assert.equal(milestone(7).current.name, '鐵')
  assert.equal(milestone(105).progress, 50)
  assert.equal(milestone(1500).current.name, '鑽石')
  assert.equal(milestone(2000).next, null)
  assert.equal(milestone(2000).progress, 100)
})
