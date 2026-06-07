<template>
  <el-tag :type="color" size="small">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { TX_STATUS_MAP, TX_STATUS_COLORS, WITHDRAW_STATUS_MAP, WITHDRAW_STATUS_COLORS } from '@/constants/status'

const props = defineProps<{ status: number; type?: 'deposit' | 'withdraw' }>()

const label = computed(() => {
  if (props.type === 'withdraw') {
    return WITHDRAW_STATUS_MAP[props.status] ?? '未知'
  }
  return TX_STATUS_MAP[props.status] ?? '未知'
})

const color = computed(() => {
  if (props.type === 'withdraw') {
    return (WITHDRAW_STATUS_COLORS[props.status] as any) ?? 'info'
  }
  return (TX_STATUS_COLORS[props.status] as any) ?? 'info'
})
</script>
