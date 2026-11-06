import { onBeforeUnmount, onMounted, ref } from 'vue'
import { today } from '../utils/activity'

// 今天的日期（yyyy-MM-dd）。頁面開過夜或從背景切回來時會更新
export function useToday () {
  const now = ref(today())
  const refresh = () => { if (now.value !== today()) now.value = today() }
  let timer
  onMounted(() => {
    timer = setInterval(refresh, 60000)
    document.addEventListener('visibilitychange', refresh)
  })
  onBeforeUnmount(() => {
    clearInterval(timer)
    document.removeEventListener('visibilitychange', refresh)
  })
  return now
}
