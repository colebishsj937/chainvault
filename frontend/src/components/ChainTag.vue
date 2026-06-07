<template>
  <span class="chain-tag">
    <el-tag :type="tagType" size="small">{{ chain }}</el-tag>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ chain: string }>()

const chainColors: Record<string, string> = {
  ETH: '',
  BTC: 'warning',
  TRON: 'danger',
  BNB: 'warning',
  MATIC: '',
}

const tagType = computed(() => (chainColors[props.chain] as any) ?? 'info')
</script>
