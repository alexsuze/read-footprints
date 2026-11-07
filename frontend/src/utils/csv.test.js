import assert from 'node:assert/strict'
import { test } from 'node:test'
import { booksToCsv } from './csv.js'
import { DEFAULT_COLUMNS, resolveColumns } from './columns.js'

const book = { title: '說"書",人', authors: ['甲', '乙'], read: true, read_date: '2026-01-05', price: 300, buy_date: '2026-01-01', reading_since: '2025-12-20' }

test('跳脫引號與逗號，加 BOM', () => {
  const csv = booksToCsv([book], true)
  assert.ok(csv.startsWith('﻿書名,'))
  assert.ok(csv.includes('"說""書"",人"'))
  assert.ok(csv.includes('甲、乙'))
  assert.ok(csv.includes(',已讀,2025-12-20,2026-01-05,2026-01-01,300'))
})

test('看別人書架不含金額、購買日期、開書日期', () => {
  const csv = booksToCsv([book], false)
  assert.ok(!csv.includes('金額'))
  assert.ok(!csv.includes('300'))
  assert.ok(!csv.includes('開書日期'))
})

test('公式開頭的內容被擋下', () => {
  assert.ok(booksToCsv([{ title: '=1+1' }], false).includes("'=1+1"))
})

test('狀態欄：未讀 / 閱讀中 / 已讀，借出另標', () => {
  assert.ok(booksToCsv([{ title: 'a', reading: true }], false).includes(',閱讀中,'))
  assert.ok(booksToCsv([{ title: 'a', loan: { status: 'lent' } }], false).includes(',未讀（借出）,'))
})

test('欄位照設定的順序，書名固定第一欄', () => {
  const csv = booksToCsv([book], true, ['price', 'authors'])
  assert.equal(csv.split('\r\n')[0], '﻿書名,金額,作者')
  assert.equal(booksToCsv([book], true, []).split('\r\n')[0], '﻿書名')
})

test('resolveColumns：預設、未知 key 略過、私人欄位', () => {
  assert.equal(resolveColumns(null, true).length, DEFAULT_COLUMNS.length + 1)
  assert.deepEqual(resolveColumns(['nope', 'isbn'], true).map(c => c.key), ['title', 'isbn'])
  assert.deepEqual(resolveColumns(['price', 'reading_since', 'buy_date', 'isbn'], false).map(c => c.key), ['title', 'isbn'])
})
