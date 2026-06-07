<template>
  <div class="page-container">
    <PageHeader title="回调日志" :parent="{ label: 'Webhook', to: '/webhook' }" />

    <el-card>
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column label="日志ID" prop="logId" width="160" />
        <el-table-column label="Webhook ID" prop="webhookId" width="160" />
        <el-table-column label="事件类型" prop="eventType" width="160" />
        <el-table-column label="URL" prop="url" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态码" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" size="small">
              {{ row.statusCode }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="80" align="right">
          <template #default="{ row }">
            {{ row.duration }}ms
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="180" />
      </el-table>

      <el-pagination class="mt-4" layout="total, sizes, prev, pager, next"
                     :total="total" :page-size="pageSize"
                     v-model:current-page="currentPage"
                     @change="load" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { getWebhookLogs } from '@/api/modules/webhook'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

async function load(page = currentPage.value) {
  loading.value = true
  try {
    const res = await getWebhookLogs({ page, size: pageSize.value })
    tableData.value = res.records
    total.value = res.total
    currentPage.value = page
  } finally {
    loading.value = false
  }
}

onMounted(() => load())
</script>
