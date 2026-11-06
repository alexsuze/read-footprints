// 書籍匯出 CSV（規劃書 4.3）：UTF-8 加 BOM，Excel 可直接開

const COLUMNS = [
  ['書名', b => b.title],
  ['ISBN', b => b.isbn],
  ['作者', b => (b.authors || []).join('、')],
  ['譯者', b => (b.translators || []).join('、')],
  ['出版社', b => b.publisher],
  ['出版日期', b => b.publish_date],
  ['分類', b => b.category_name],
  ['字數', b => b.word_count],
  ['已讀', b => (b.read ? '是' : '否')],
  ['完讀日期', b => b.read_date],
  ['購買連結', b => b.buy_url]
]
const PRIVATE_COLUMNS = [['金額', b => b.price], ['購買日期', b => b.buy_date]]

function cell (value) {
  let s = value == null ? '' : String(value)
  if (/^[=+\-@\t\r]/.test(s)) s = `'${s}` // 擋 Excel 把書名當公式執行
  return /[",\r\n]/.test(s) ? `"${s.replaceAll('"', '""')}"` : s
}

// showPrivate：自己的書架才含金額與購買日期
export function booksToCsv (books, showPrivate) {
  const columns = showPrivate ? [...COLUMNS, ...PRIVATE_COLUMNS] : COLUMNS
  const rows = [columns.map(c => c[0]), ...books.map(b => columns.map(c => cell(c[1](b))))]
  return '﻿' + rows.map(r => r.join(',')).join('\r\n')
}
