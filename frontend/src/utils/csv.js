// 書籍匯出 CSV（規劃書 4.3）：UTF-8 加 BOM，Excel 可直接開。欄位跟著設定 → 報表欄位
import { resolveColumns } from './columns.js'

function cell (value) {
  let s = value == null ? '' : String(value)
  if (/^[=+\-@\t\r]/.test(s)) s = `'${s}` // 擋 Excel 把書名當公式執行
  return /[",\r\n]/.test(s) ? `"${s.replaceAll('"', '""')}"` : s
}

// isOwner：自己的書架才含開書日期、購買日期、金額；keys：使用者選的欄位（null = 預設）
export function booksToCsv (books, isOwner, keys = null) {
  const columns = resolveColumns(keys, isOwner)
  const rows = [columns.map(c => c.label), ...books.map(b => columns.map(c => cell(c.text(b))))]
  return '﻿' + rows.map(r => r.join(',')).join('\r\n')
}
