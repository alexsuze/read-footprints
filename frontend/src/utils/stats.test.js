import { test } from 'node:test'
import assert from 'node:assert/strict'
import { funStats, wan, logMonths, minutesByDay, monthSummary, completedByPeriod, missingLogDates, shiftDate, formatMinutes, milestone, minutesByMonth, minutesByYear, ranking, spending, spendingByPeriod } from './stats.js'

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

test('前 7 天未填的日期，跨月、舊到新、不含今天', () => {
  assert.equal(shiftDate('2026-03-01', -1), '2026-02-28')
  assert.deepEqual(missingLogDates(['2026-02-24', '2026-02-26', '2026-02-27', '2026-02-28', '2026-03-01'], '2026-03-01'),
    ['2026-02-22', '2026-02-23', '2026-02-25'])
  assert.deepEqual(missingLogDates([], '2026-11-06').length, 7)
  assert.deepEqual(missingLogDates(['2026-10-30', '2026-10-31', '2026-11-01', '2026-11-02', '2026-11-03', '2026-11-04'], '2026-11-06'), ['2026-11-05'])
})

test('每日時數：整個月、今天之後留空、小結與月份選單', () => {
  const logs = [{ date: '2026-11-01', minutes: 30 }, { date: '2026-11-03', minutes: 0 }, { date: '2026-11-05', minutes: 50 }, { date: '2026-02-10', minutes: 20 }]
  const days = minutesByDay(logs, '2026-11', '2026-11-07')
  assert.equal(days.length, 30)
  assert.deepEqual(days.slice(0, 3).map(d => d.value), [30, 0, 0])
  assert.equal(days[6].value, 0)
  assert.equal(days[7].value, null)
  assert.equal(minutesByDay(logs, '2026-02', '2026-11-07').length, 28)
  assert.deepEqual(monthSummary(days), { total: 80, readDays: 2, average: 40 })
  assert.deepEqual(monthSummary([]), { total: 0, readDays: 0, average: 0 })
  assert.deepEqual(logMonths(logs, '2026-12-01'), ['2026-12', '2026-11', '2026-02'])
})

test('funStats：一字千金、速度、讀完書架', () => {
  const f = funStats({
    total_minutes: 3000, reading_days: 60,
    word_price: { books: 2, total_price: 700, total_words: 150000, cheapest: { id: 'a', title: 'A', price: 300, words: 100000 }, priciest: { id: 'b', title: 'B', price: 400, words: 50000 } },
    speed: { books: 2, median_wpm: 300, total_words: 36000, total_minutes: 120, total_days: 4, details: [] },
    backlog: { books_with_words: 2, words: 150000, books_without_words: 1 }
  })
  assert.ok(Math.abs(f.wordPrice.perThousand - 4.6667) < 1e-3)
  assert.ok(Math.abs(f.wordPrice.wordsPerDollar - 214.29) < 0.01)
  assert.equal(f.wordPrice.cheapest.perThousand, 3)
  assert.equal(f.wordPrice.priciest.perThousand, 8)
  assert.equal(f.speed.wpm, 300)
  assert.equal(f.speed.wpd, 9000)
  // 150000 字 ÷ 300 字/分 ÷ 50 分/天 = 10 天
  assert.equal(f.backlog.days, 10)
  assert.equal(f.backlog.avgDaily, 50)
})

test('funStats：沒有資料時為 null', () => {
  const f = funStats({ total_minutes: 0, reading_days: 0, word_price: { books: 0 }, speed: { books: 0 }, backlog: { books_with_words: 0, words: 0, books_without_words: 3 } })
  assert.equal(f.wordPrice, null)
  assert.equal(f.speed, null)
  assert.equal(f.backlog.days, null)
  assert.equal(wan(12345), '1.2 萬字')
  assert.equal(wan(980), '980 字')
})
