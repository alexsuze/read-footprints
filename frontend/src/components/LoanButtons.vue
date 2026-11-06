<template>
  <button
    v-for="a in actions"
    :key="a"
    type="button"
    class="btn text-nowrap"
    :class="[size ? `btn-${size}` : '', BUTTONS[a].style]"
    :disabled="busy"
    @click="run(a)"
  >{{ BUTTONS[a].label }}</button>
</template>

<script setup>
import { computed, ref } from 'vue'
import http from '../api/http'
import { toast } from './AppToast.vue'
import { useAuthStore } from '../stores/auth'
import { loanActions } from '../utils/loan'

// 借書 / 取消申請 / 歸還 / 核定借出 / 拒絕 / 收回（攤開書籍、總覽租借中、租借管理共用）
// book 模式：傳入書（含 loan），完成後直接更新 book.loan；loan 模式：傳入借閱紀錄（總覽、租借管理）
const props = defineProps({
  book: { type: Object, default: null },
  loan: { type: Object, default: null },
  size: { type: String, default: '' }
})
const emit = defineEmits(['changed'])

const BUTTONS = {
  request: { label: '借書', style: 'btn-primary', ok: '已送出借書申請，請等管理員核定' },
  cancel: { label: '取消申請', style: 'btn-outline-danger', confirm: '取消這筆借書申請？', ok: '已取消申請' },
  return: { label: '歸還', style: 'btn-primary', confirm: '確定已經把書還給管理員了嗎？', ok: '已歸還，謝謝' },
  lend: { label: '核定借出', style: 'btn-primary', ok: '已核定借出' },
  reject: { label: '拒絕', style: 'btn-outline-danger', confirm: '拒絕這筆借書申請？', ok: '已拒絕申請' },
  reclaim: { label: '收回', style: 'btn-outline-danger', confirm: '確定收回這本書？', ok: '已收回' }
}
const PATHS = { cancel: 'return', return: 'return', lend: 'lend', reject: 'reclaim', reclaim: 'reclaim' }

const auth = useAuthStore()
const busy = ref(false)

const current = computed(() => (props.book
  ? props.book.loan
  : props.loan && { ...props.loan, mine: props.loan.borrower_id === auth.user?.id }))
const actions = computed(() => {
  const isOwner = auth.isAdmin
  const canBorrow = !!props.book && !isOwner && props.book.user_id !== auth.user?.id
  return loanActions(current.value, { isOwner, canBorrow })
})

async function run (action) {
  const b = BUTTONS[action]
  if (b.confirm && !confirm(b.confirm)) return
  busy.value = true
  try {
    const { data } = action === 'request'
      ? await http.post('/loans', { book_id: props.book.id })
      : await http.post(`/loans/${current.value.id}/${PATHS[action]}`)
    if (props.book) {
      props.book.loan = data.status === 'returned' ? null : { id: data.id, status: data.status, mine: data.borrower_id === auth.user?.id, borrower_name: auth.isAdmin ? data.borrower_name : null }
    }
    toast(b.ok)
    emit('changed', data)
  } finally {
    busy.value = false
  }
}
</script>
