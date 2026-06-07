<template>
  <div class="page-header">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item v-if="parent" :to="parent.to">{{ parent.label }}</el-breadcrumb-item>
      <el-breadcrumb-item>{{ title }}</el-breadcrumb-item>
    </el-breadcrumb>
    <div class="page-title-row">
      <h3 class="page-title">{{ title }}</h3>
      <div v-if="$slots.extra" class="page-extra">
        <slot name="extra" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; parent?: { label: string; to: string } }>()
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
}
.page-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
}
.page-title {
  margin: 0;
}
</style>
