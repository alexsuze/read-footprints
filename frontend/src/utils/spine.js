// 書脊外觀（規劃書第 3 節）。純函式，node --test 可測

const PALETTE = ['#5B6B7A', '#B5493A', '#7A8C5C', '#2F3C5C', '#C89B3C', '#6B5B95', '#C4713B', '#5F9E7A', '#4A4844', '#8B6B4A']
const MIN_WIDTH = 26
const MAX_WIDTH = 64
const MID_WIDTH = 44
const MIN_WORDS = 50000
const MAX_WORDS = 600000

function hash (text) {
  let h = 0
  for (const ch of text || '') h = (h * 31 + ch.codePointAt(0)) >>> 0
  return h
}

// 優先用書封算出的顏色，沒有書封才用書名雜湊
export const spineColor = book => book.spine_color || PALETTE[hash(book.title) % PALETTE.length]

// 字數線性對應厚度，沒有字數用中間值
export function spineWidth (wordCount) {
  if (!wordCount) return MID_WIDTH
  const ratio = (wordCount - MIN_WORDS) / (MAX_WORDS - MIN_WORDS)
  return Math.round(MIN_WIDTH + (MAX_WIDTH - MIN_WIDTH) * Math.min(Math.max(ratio, 0), 1))
}

// 同一本書高度固定，整排才有高低錯落
export const spineHeight = title => 170 + hash(title) % 19

// 底色偏亮用墨色字，其餘用紙色字
export function spineInk (hex) {
  const [r, g, b] = [1, 3, 5].map(i => parseInt(hex.slice(i, i + 2), 16))
  return (0.299 * r + 0.587 * g + 0.114 * b) / 255 > 0.6 ? '#2B2A28' : '#F7F1E3'
}
