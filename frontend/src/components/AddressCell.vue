<template>
  <div class="address-cell">
    <el-tooltip :content="address" placement="top">
      <span class="mono address-text">{{ truncated }}</span>
    </el-tooltip>
    <el-button link :icon="CopyDocument" size="small" @click.stop="copy" />
    <el-button v-if="explorerUrl" link :icon="Link" size="small" @click.stop="openExplorer" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CopyDocument, Link } from '@element-plus/icons-vue'
import { getExplorerAddressUrl } from '@/constants/chains'
import { copyToClipboard } from '@/utils/clipboard'

const props = defineProps<{ address: string; chain?: string }>()

const truncated = computed(() =>
  props.address.length > 12
    ? `${props.address.slice(0, 6)}...${props.address.slice(-4)}`
    : props.address,
)

const explorerUrl = computed(() =>
  props.chain ? getExplorerAddressUrl(props.chain, props.address) : '',
)

function copy() {
  copyToClipboard(props.address)
}

function openExplorer() {
  window.open(explorerUrl.value, '_blank', 'noopener')
}
</script>

<style scoped>
.address-cell {
  display: flex;
  align-items: center;
  gap: 2px;
}
.address-text {
  font-size: 13px;
  max-width: 120px;
  overflow: hidden;
  white-space: nowrap;
}
.mono {
  font-family: 'Courier New', Courier, monospace;
}
</style>
