<template>
  <div class="page-container">
    <PageHeader title="风控规则" />
    <ProLock desc="风控规则引擎需要商业版授权，支持金额上限、地址黑名单、频率限制">
      <el-card>
        <el-empty description="风控规则配置（Pro 功能）" />
      </el-card>
    </ProLock>
  </div>
</template>

<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
import ProLock from '@/components/ProLock.vue'
</script>
