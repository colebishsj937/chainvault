<template>
  <el-button link :icon="CopyDocument" size="small" @click.stop="copy">
    <slot />
  </el-button>
</template>

<script setup lang="ts">
import { CopyDocument } from '@element-plus/icons-vue'
import { copyToClipboard } from '@/utils/clipboard'

const props = defineProps<{ text: string }>()

function copy() {
  copyToClipboard(props.text)
}
</script>
