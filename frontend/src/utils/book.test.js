import { test } from 'node:test'
import assert from 'node:assert/strict'
import { bookState, clampPage, readingDay } from './book.js'

test('bookState', () => {
  assert.equal(bookState({ read: true, reading: false }), 'read')
  assert.equal(bookState({ read: false, reading: true }), 'reading')
  assert.equal(bookState({ read: false }), 'unread')
})

test('readingDay 開書當天是第 1 天', () => {
  assert.equal(readingDay('2026-11-06', '2026-11-06'), 1)
  assert.equal(readingDay('2026-10-31', '2026-11-06'), 7)
  assert.equal(readingDay(null, '2026-11-06'), null)
})

test('clampPage', () => {
  assert.equal(clampPage('3', 12), 3)
  assert.equal(clampPage('99', 12), 12)
  assert.equal(clampPage('0', 12), 1)
  assert.equal(clampPage('-5', 12), 1)
  assert.equal(clampPage('2.6', 12), 3)
  assert.equal(clampPage('abc', 12), null)
  assert.equal(clampPage('', 12), null)
})
