import { test } from 'node:test'
import assert from 'node:assert/strict'
import { formatTime, loanActions } from './loan.js'

test('loanActions 依身分與狀態', () => {
  assert.deepEqual(loanActions(null, { isOwner: false, canBorrow: true }), ['request'])
  assert.deepEqual(loanActions(null, { isOwner: false, canBorrow: false }), [])
  assert.deepEqual(loanActions(null, { isOwner: true, canBorrow: false }), [])
  assert.deepEqual(loanActions({ status: 'requested' }, { isOwner: true }), ['lend', 'reject'])
  assert.deepEqual(loanActions({ status: 'lent' }, { isOwner: true }), ['reclaim'])
  assert.deepEqual(loanActions({ status: 'requested', mine: true }, { isOwner: false }), ['cancel'])
  assert.deepEqual(loanActions({ status: 'lent', mine: true }, { isOwner: false }), ['return'])
  assert.deepEqual(loanActions({ status: 'returned' }, { isOwner: true }), [])
  assert.deepEqual(loanActions({ status: 'returned', mine: true }, { isOwner: false }), [])
  assert.deepEqual(loanActions({ status: 'lent', mine: false }, { isOwner: false, canBorrow: true }), [])
})

test('formatTime', () => {
  assert.equal(formatTime(null), '')
  assert.match(formatTime('2026-11-06T08:09:12.650Z'), /^2026\/11\/0\d \d\d:\d\d$/)
})
