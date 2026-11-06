import assert from 'node:assert/strict'
import { test } from 'node:test'
import { spineColor, spineHeight, spineInk, spineWidth } from './spine.js'

test('字數決定厚度，超出範圍夾住，沒有字數用中間值', () => {
  assert.equal(spineWidth(null), 44)
  assert.equal(spineWidth(1000), 26)
  assert.equal(spineWidth(9000000), 64)
  assert.ok(spineWidth(200000) > 26 && spineWidth(200000) < spineWidth(400000))
})

test('有書封主色就用，沒有時同書名得到同顏色', () => {
  assert.equal(spineColor({ title: '沙丘', spine_color: '#123456' }), '#123456')
  assert.equal(spineColor({ title: '沙丘' }), spineColor({ title: '沙丘' }))
  assert.match(spineColor({ title: '' }), /^#[0-9A-F]{6}$/)
})

test('高度落在範圍內且固定', () => {
  assert.equal(spineHeight('三體'), spineHeight('三體'))
  assert.ok(spineHeight('三體') >= 170 && spineHeight('三體') <= 188)
})

test('字色依底色明度', () => {
  assert.equal(spineInk('#EEDDCC'), '#2B2A28')
  assert.equal(spineInk('#2F3C5C'), '#F7F1E3')
})
