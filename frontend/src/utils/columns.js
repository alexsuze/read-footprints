// 報表欄位（圖書館列表與 CSV 匯出共用）。書名固定在第一欄，其他由使用者在設定裡選與排序
import { bookState, STATE_LABELS } from './book.js'

const join = list => (list || []).join('、')

// sort：對應 /api/books 的 sort 參數；private：只有書主看得到（看管理員書架時一律不出現）
export const COLUMNS = [
  { key: 'isbn', label: 'ISBN', text: b => b.isbn },
  { key: 'authors', label: '作者', text: b => join(b.authors), sort: 'author' },
  { key: 'translators', label: '譯者', text: b => join(b.translators) },
  { key: 'publisher', label: '出版社', text: b => b.publisher, sort: 'publisher' },
  { key: 'publish_date', label: '出版日期', text: b => b.publish_date, sort: 'publish_date' },
  { key: 'category', label: '分類', text: b => b.category_name || '未分類' },
  { key: 'word_count', label: '字數', text: b => b.word_count, align: 'end' },
  { key: 'state', label: '狀態', text: b => STATE_LABELS[bookState(b)] + (b.loan?.status === 'lent' ? '（借出）' : '') },
  { key: 'reading_since', label: '開書日期', text: b => b.reading_since, sort: 'reading_since', private: true },
  { key: 'read_date', label: '完讀日期', text: b => b.read_date, sort: 'read_date' },
  { key: 'buy_date', label: '購買日期', text: b => b.buy_date, sort: 'buy_date', private: true },
  { key: 'price', label: '金額', text: b => b.price, sort: 'price', private: true, align: 'end' },
  { key: 'buy_url', label: '購買連結', text: b => b.buy_url },
  { key: 'created_at', label: '建立時間', text: b => (b.created_at ? new Date(b.created_at).toLocaleDateString('sv') : ''), sort: 'created_at' }
]

export const TITLE_COLUMN = { key: 'title', label: '書名', text: b => b.title, sort: 'title' }

export const DEFAULT_COLUMNS = ['authors', 'publisher', 'category', 'state', 'reading_since', 'read_date', 'buy_date', 'price']

// 依使用者設定（null = 預設）排出要顯示的欄位，書名永遠在最前；看別人的書架時拿掉私人欄位
export function resolveColumns (keys, isOwner) {
  const byKey = new Map(COLUMNS.map(c => [c.key, c]))
  const picked = (keys ?? DEFAULT_COLUMNS).map(k => byKey.get(k)).filter(Boolean)
  return [TITLE_COLUMN, ...picked.filter(c => isOwner || !c.private)]
}
