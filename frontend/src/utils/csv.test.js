import assert from 'node:assert/strict'
import { test } from 'node:test'
import { booksToCsv } from './csv.js'

const book = { title: '說"書",人', authors: ['甲', '乙'], read: true, price: 300, buy_date: '2026-01-01', reading_since: '2025-12-20' }

test('跳脫引號與逗號，加 BOM', () => {
  const csv = booksToCsv([book], true)
  assert.ok(csv.startsWith('﻿書名,'))
  assert.ok(csv.includes('"說""書"",人"'))
  assert.ok(csv.includes('甲、乙'))
  assert.ok(csv.endsWith('300,2026-01-01,2025-12-20'))
  assert.ok(csv.includes(',已讀,'))
})

test('看別人書架不含金額與購買日期', () => {
  const csv = booksToCsv([book], false)
  assert.ok(!csv.includes('金額'))
  assert.ok(!csv.includes('300'))
  assert.ok(!csv.includes('開書日期'))
})

test('公式開頭的內容被擋下', () => {
  assert.ok(booksToCsv([{ title: '=1+1' }], false).includes("'=1+1"))
})

test('狀態欄：未讀 / 閱讀中 / 已讀', () => {
  assert.ok(booksToCsv([{ title: 'a', reading: true }], false).includes(',閱讀中,'))
  assert.ok(booksToCsv([{ title: 'a' }], false).includes(',未讀,'))
})
