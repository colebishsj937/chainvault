<template>
  <div class="hash-link">
    <el-tooltip :content="hash" placement="top">
      <span class="mono hash-text">{{ truncated }}</span>
    </el-tooltip>
    <el-button link :icon="CopyDocument" size="small" @click.stop="copy" />
    <el-button v-if="explorerUrl" link :icon="Link" size="small" @click.stop="openExplorer" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CopyDocument, Link } from '@element-plus/icons-vue'
import { getExplorerTxUrl } from '@/constants/chains'
import { copyToClipboard } from '@/utils/clipboard'

const props = defineProps<{ hash: string; chain?: string }>()

const truncated = computed(() =>
  props.hash.length > 12
    ? `${props.hash.slice(0, 6)}...${props.hash.slice(-4)}`
    : props.hash,
)

const explorerUrl = computed(() =>
  props.chain ? getExplorerTxUrl(props.chain, props.hash) : '',
)

function copy() {
  copyToClipboard(props.hash)
}

function openExplorer() {
  window.open(explorerUrl.value, '_blank', 'noopener')
}
</script>

<style scoped>
.hash-link {
  display: flex;
  align-items: center;
  gap: 2px;
}
.hash-text {
  font-size: 13px;
  max-width: 100px;
  overflow: hidden;
  white-space: nowrap;
}
.mono {
  font-family: 'Courier New', Courier, monospace;
}
</style>
