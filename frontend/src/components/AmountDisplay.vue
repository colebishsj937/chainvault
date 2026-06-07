<template>
  <el-tooltip :content="fullAmount" placement="top" :disabled="!showTooltip">
    <span :class="['amount', { 'text-success': positive, 'text-danger': negative }]">
      {{ displayAmount }} {{ symbol }}
    </span>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatAmount } from '@/utils/amount'

const props = defineProps<{
  amount: string | number
  symbol?: string
  decimals?: number
  positive?: boolean
  negative?: boolean
}>()

// 接口可能返回 number，统一转为字符串再格式化
const amountText = computed(() => {
  if (props.amount === null || props.amount === undefined || props.amount === '') {
    return ''
  }
  return String(props.amount)
})

const displayAmount = computed(() => formatAmount(amountText.value, 4))
const fullAmount = computed(() => formatAmount(amountText.value, 18))
const showTooltip = computed(
  () => amountText.value.includes('.') && (amountText.value.split('.')[1]?.length ?? 0) > 4,
)
</script>

<style scoped>
.amount {
  font-variant-numeric: tabular-nums;
}
.text-success {
  color: #67c23a;
}
.text-danger {
  color: #f56c6c;
}
</style>
