// 借書（規劃書第 5 節）共用文字與格式

export const LOAN_LABELS = { requested: '申請中', lent: '借出中', returned: '已結束' }
export const ENDED_LABELS = { borrower: '借書人歸還', owner: '管理員收回' }

// 後端時間是 UTC Instant，顯示成本地 yyyy/MM/dd HH:mm
export function formatTime (instant) {
  if (!instant) return ''
  const d = new Date(instant)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 依身分與借閱狀態決定可按的按鈕。isOwner：目前使用者是書主（管理員）
// 申請中被管理員收回 = 拒絕；借書人對申請中按歸還 = 取消申請
export function loanActions (loan, { isOwner, canBorrow }) {
  if (!loan) return canBorrow ? ['request'] : []
  if (loan.status === 'returned') return []
  if (isOwner) return loan.status === 'requested' ? ['lend', 'reject'] : ['reclaim']
  if (loan.mine) return loan.status === 'requested' ? ['cancel'] : ['return']
  return []
}
