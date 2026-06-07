<template>
  <div class="page-container">
    <PageHeader title="Webhook 配置" :parent="{ label: 'Webhook', to: '/webhook' }" />

    <el-card>
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增配置</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column label="ID" prop="webhookId" width="160" />
        <el-table-column label="商户" prop="merchantId" width="160" />
        <el-table-column label="URL" prop="url" min-width="200" show-overflow-tooltip />
        <el-table-column label="事件类型" width="160">
          <template #default="{ row }">
            <el-tag v-for="ev in row.events" :key="ev" size="small" class="mr-1">
              {{ ev }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled" disabled />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTest(row)">测试</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.webhookId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="mt-4" layout="total, sizes, prev, pager, next"
                     :total="total" :page-size="pageSize"
                     v-model:current-page="currentPage"
                     @change="load" />
    </el-card>

    <el-dialog v-model="formVisible" :title="editing ? '编辑 Webhook' : '新增 Webhook'" width="520px">
      <el-form :model="form" ref="formRef" :rules="formRules" label-width="100px">
        <el-form-item label="URL" prop="url">
          <el-input v-model="form.url" placeholder="https://your-api.com/callback" />
        </el-form-item>
        <el-form-item label="密钥" prop="secret">
          <el-input v-model="form.secret" placeholder="Webhook 签名密钥" />
        </el-form-item>
        <el-form-item label="事件类型" prop="events">
          <el-select v-model="form.events" multiple placeholder="选择事件类型">
            <el-option label="deposit.confirmed" value="deposit.confirmed" />
            <el-option label="withdraw.success" value="withdraw.success" />
            <el-option label="withdraw.failed" value="withdraw.failed" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="testVisible" title="测试 Webhook 推送" width="520px">
      <el-form :model="testForm" label-width="80px">
        <el-form-item label="事件类型">
          <el-select v-model="testForm.eventType">
            <el-option label="deposit.confirmed" value="deposit.confirmed" />
            <el-option label="withdraw.success" value="withdraw.success" />
            <el-option label="withdraw.failed" value="withdraw.failed" />
          </el-select>
        </el-form-item>
        <el-form-item label="测试数据">
          <el-input v-model="testForm.payload" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <div v-if="testResult" class="test-result">
        <el-tag :type="testResult.success ? 'success' : 'danger'">
          {{ testResult.success ? '推送成功' : '推送失败' }}
          HTTP {{ testResult.statusCode }}
        </el-tag>
        <div class="result-body mono">{{ testResult.responseBody }}</div>
        <div class="result-time text-secondary">耗时 {{ testResult.duration }}ms</div>
      </div>
      <template #footer>
        <el-button @click="testVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testing" @click="sendTest">发送测试</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import {
  getWebhookList, createWebhook, updateWebhook, deleteWebhook, testWebhook,
} from '@/api/modules/webhook'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

async function load(page = currentPage.value) {
  loading.value = true
  try {
    const res = await getWebhookList({ page, size: pageSize.value })
    tableData.value = res.records
    total.value = res.total
    currentPage.value = page
  } finally {
    loading.value = false
  }
}

const formVisible = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  webhookId: '',
  url: '',
  secret: '',
  events: [] as string[],
  enabled: true,
})
const formRules = {
  url: [{ required: true, message: '请输入 URL', trigger: 'blur' }],
  events: [{ required: true, message: '请选择事件类型', trigger: 'change' }],
}

function openCreate() {
  editing.value = false
  form.webhookId = ''
  form.url = ''
  form.secret = ''
  form.events = []
  form.enabled = true
  formVisible.value = true
}

function openEdit(row: any) {
  editing.value = true
  form.webhookId = row.webhookId
  form.url = row.url
  form.secret = row.secret
  form.events = [...row.events]
  form.enabled = row.enabled
  formVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) { return }
  saving.value = true
  try {
    if (editing.value) {
      await updateWebhook(form.webhookId, form)
      ElMessage.success('更新成功')
    } else {
      await createWebhook(form)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handleDelete(webhookId: string) {
  try {
    await ElMessageBox.confirm('确认删除此 Webhook？', '确认删除', { type: 'warning' })
  } catch {
    return
  }
  await deleteWebhook(webhookId)
  ElMessage.success('已删除')
  load()
}

const testVisible = ref(false)
const testing = ref(false)
const testResult = ref<any>(null)
const testForm = reactive({
  webhookId: '',
  eventType: 'deposit.confirmed',
  payload: JSON.stringify({
    event: 'deposit.confirmed', amount: '10.5', symbol: 'USDT',
    txHash: '0xtest...', confirms: 12,
  }, null, 2),
})

function openTest(row: any) {
  testForm.webhookId = row.webhookId
  testResult.value = null
  testVisible.value = true
}

async function sendTest() {
  testing.value = true
  testResult.value = null
  try {
    testResult.value = await testWebhook(testForm)
  } finally {
    testing.value = false
  }
}

onMounted(() => load())
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
.test-result {
  margin-top: 16px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 4px;
}
.result-body {
  max-height: 120px;
  overflow-y: auto;
  font-size: 12px;
  margin-top: 8px;
}
.result-time {
  font-size: 12px;
  margin-top: 4px;
}
.mr-1 {
  margin-right: 4px;
}
</style>
