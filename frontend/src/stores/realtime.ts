import { defineStore } from 'pinia'
import { ref } from 'vue'
import { sseManager } from '@/utils/sse'
import { ElNotification } from 'element-plus'

export const useRealtimeStore = defineStore('realtime', () => {
  const pendingCount = ref(0)

  function init(merchantId: string) {
    sseManager.connect(`/api/v1/sse?merchantId=${merchantId}`)

    sseManager.on('deposit.confirmed', (data: any) => {
      pendingCount.value++
      ElNotification({
        title: '充值到账',
        message: `${data.symbol} ${data.amount} 已确认 ${data.confirms} 次`,
        type: 'success',
        duration: 5000,
      })
    })

    sseManager.on('withdraw.success', (data: any) => {
      ElNotification({
        title: '提币成功',
        message: `订单 ${data.orderNo} 已上链`,
        type: 'success',
      })
    })
  }

  return { pendingCount, init }
})
