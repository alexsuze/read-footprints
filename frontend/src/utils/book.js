// 書的閱讀狀態：未讀 → 閱讀中（開書）→ 已讀；放棄 = 回到未讀

export const STATE_LABELS = { unread: '未讀', reading: '閱讀中', read: '已讀' }

export const bookState = b => (b.read ? 'read' : b.reading ? 'reading' : 'unread')

// 開書第幾天（開書當天算第 1 天）
export function readingDay (since, today) {
  if (!since) return null
  return Math.round((Date.parse(today) - Date.parse(since)) / 86400000) + 1
}

// 翻頁輸入：轉成 1..pages 的整數，不是數字回傳 null
export function clampPage (value, pages) {
  const n = Math.round(Number(String(value).trim()))
  if (!String(value).trim() || !Number.isFinite(n)) return null
  return Math.min(Math.max(n, 1), Math.max(pages, 1))
}
