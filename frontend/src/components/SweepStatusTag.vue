<template>
  <el-tag :type="color" size="small">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  SWEEP_BATCH_STATUS_MAP,
  SWEEP_BATCH_STATUS_COLORS,
  SWEEP_RECORD_STATUS_MAP,
  SWEEP_RECORD_STATUS_COLORS,
} from '@/constants/sweep'

const props = defineProps<{ status: number; type?: 'batch' | 'record' }>()

const label = computed(() => {
  if (props.type === 'batch') {
    return SWEEP_BATCH_STATUS_MAP[props.status] ?? '未知'
  }
  return SWEEP_RECORD_STATUS_MAP[props.status] ?? '未知'
})

const color = computed(() => {
  if (props.type === 'batch') {
    return (SWEEP_BATCH_STATUS_COLORS[props.status] as any) ?? 'info'
  }
  return (SWEEP_RECORD_STATUS_COLORS[props.status] as any) ?? 'info'
})
</script>
