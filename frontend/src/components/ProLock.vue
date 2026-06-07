<template>
  <div class="pro-lock-wrapper">
    <div :class="{ 'content-blur': locked }">
      <slot />
    </div>

    <div v-if="locked" class="lock-overlay">
      <el-icon :size="32" class="mb-2"><Lock /></el-icon>
      <p class="lock-title">Pro 版功能</p>
      <p class="lock-desc">{{ desc }}</p>
      <el-button type="warning" size="small" @click="$emit('upgrade')">
        升级商业版
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Lock } from '@element-plus/icons-vue'
import { useMerchantStore } from '@/stores/merchant'

const props = defineProps<{ desc?: string }>()
defineEmits(['upgrade'])

const merchant = useMerchantStore()
const locked = computed(() => merchant.tier === 0)
</script>

<style scoped>
.pro-lock-wrapper {
  position: relative;
}
.content-blur {
  filter: blur(3px);
  pointer-events: none;
  user-select: none;
}
.lock-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 8px;
  gap: 8px;
}
.lock-title {
  font-weight: 600;
  font-size: 15px;
}
.lock-desc {
  font-size: 13px;
  color: #666;
  text-align: center;
}
.mb-2 {
  margin-bottom: 8px;
}
</style>
