<template>
  <div class="page-container">
    <PageHeader title="商户详情" :parent="{ label: '商户管理', to: '/merchant' }">
      <template #extra>
        <el-button type="primary" link @click="router.push('/merchant-api-docs')">
          查看接口文档
        </el-button>
      </template>
    </PageHeader>

    <el-card v-loading="loading" class="mb-4">
      <template #header>基本信息</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="商户ID">{{ merchant.merchantId }}</el-descriptions-item>
        <el-descriptions-item label="商户名称">{{ merchant.merchantName }}</el-descriptions-item>
        <el-descriptions-item label="版本">
          <el-tag :type="merchant.tier === 1 ? 'warning' : 'info'">
            {{ merchant.tier === 1 ? '商业版' : '开源版' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ merchant.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card>
      <template #header>
        <span>API Key 管理</span>
        <el-button type="warning" size="small" style="margin-left:16px" @click="handleReset" :loading="resetting">
          重置 API Key
        </el-button>
      </template>

      <el-alert type="warning" :closable="false" class="mb-4">
        Secret Key 仅在重置时显示一次，请立即保存。
      </el-alert>

      <div v-if="newKeys" class="key-display mb-4">
        <p><strong>新 Secret Key:</strong> <code>{{ newKeys.secretKey }}</code>
          <CopyButton :text="newKeys.secretKey" />
        </p>
        <el-alert type="error" :closable="false" class="mt-2">
          此密钥仅在本次显示，请立即复制保存！
        </el-alert>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import CopyButton from '@/components/CopyButton.vue'
import { getMerchant, rotateMerchantSecret } from '@/api/modules/merchant'
import type { Merchant, ApiKeyResetResult } from '@/api/types/merchant'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const resetting = ref(false)
const merchant = ref<Merchant>({} as Merchant)
const newKeys = ref<ApiKeyResetResult | null>(null)

async function loadMerchant() {
  loading.value = true
  try {
    merchant.value = await getMerchant(route.params.id as string)
  } finally {
    loading.value = false
  }
}

async function handleReset() {
  try {
    await ElMessageBox.confirm('重置后旧 API Key 将立即失效，确认继续？', '警告', {
      type: 'warning',
    })
  } catch {
    return
  }
  resetting.value = true
  try {
    newKeys.value = await rotateMerchantSecret(route.params.id as string)
    ElMessage.success('密钥已轮换')
  } finally {
    resetting.value = false
  }
}

onMounted(() => loadMerchant())
</script>
