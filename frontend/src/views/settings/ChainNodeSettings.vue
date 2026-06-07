<template>
  <div class="page-container">
    <PageHeader title="链节点配置" />

    <el-alert
      class="mb-4"
      type="info"
      :closable="false"
      show-icon
      title="配置保存后约 30 秒内自动生效；Gateway 会优先使用数据库配置，未配置时回退环境变量。多个 API Key 将按请求轮询使用，分散额度。"
    />

    <el-card>
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column label="链" prop="chainCode" width="90">
          <template #default="{ row }">
            <ChainTag :chain="row.chainCode" />
          </template>
        </el-table-column>
        <el-table-column label="服务商" width="140">
          <template #default="{ row }">
            {{ providerLabel(row.provider) }}
          </template>
        </el-table-column>
        <el-table-column label="API Key" width="120">
          <template #default="{ row }">
            <span v-if="needsApiKey(row as ChainNodeConfig)">
              {{ (row as ChainNodeConfig).apiKeyCount
                ? `${(row as ChainNodeConfig).apiKeyCount} 个`
                : '未配置' }}
            </span>
            <span v-else class="text-secondary">—</span>
          </template>
        </el-table-column>
        <el-table-column label="有效端点" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="mono">{{ displayEndpoint(row as ChainNodeConfig) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="确认数" prop="requiredConfirms" width="80" />
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled === 1 ? 'success' : 'info'" size="small">
              {{ row.isEnabled === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="扫块就绪" width="100">
          <template #default="{ row }">
            <el-tag :type="row.scanReady ? 'success' : 'warning'" size="small">
              {{ row.scanReady ? '就绪' : '未就绪' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" prop="updatedAt" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row as ChainNodeConfig)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="`编辑 ${editing?.chainCode} 节点`" width="600px">
      <el-form :model="form" ref="formRef" :rules="formRules" label-width="110px">
        <el-form-item label="服务商" prop="provider">
          <el-select v-model="form.provider" placeholder="选择服务商" style="width: 100%">
            <el-option
              v-for="opt in providerOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="needsApiKeyForm" label="API Key">
          <div class="api-key-panel">
            <div v-if="apiKeyList.length" class="api-key-list">
              <div v-for="item in apiKeyList" :key="item.id" class="api-key-row">
                <span class="mono">{{ item.apiKeyMasked }}</span>
                <span v-if="item.label" class="api-key-label">{{ item.label }}</span>
                <el-button
                  link
                  type="danger"
                  :loading="deletingKeyId === item.id"
                  @click="handleDeleteApiKey(item.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
            <el-empty v-else description="暂无 API Key" :image-size="48" />
            <div class="api-key-add">
              <el-input
                v-model="newApiKey"
                type="password"
                show-password
                placeholder="输入新 API Key"
                class="api-key-input"
              />
              <el-input
                v-model="newApiKeyLabel"
                placeholder="备注（可选）"
                class="api-key-label-input"
              />
              <el-button type="primary" plain :loading="addingKey" @click="handleAddApiKey">
                添加
              </el-button>
            </div>
            <p class="api-key-hint">请求时将自动轮询使用已添加的 Key，避免单 Key 额度耗尽。</p>
          </div>
        </el-form-item>

        <el-form-item v-if="needsRpcUrl" label="RPC 地址" prop="rpcUrl">
          <el-input v-model="form.rpcUrl" placeholder="https://..." />
        </el-form-item>

        <el-form-item v-if="isTron" label="API 地址" prop="apiUrl">
          <el-input v-model="form.apiUrl" placeholder="https://api.trongrid.io" />
        </el-form-item>

        <template v-if="isBtc">
          <el-form-item label="RPC 用户名" prop="rpcUser">
            <el-input v-model="form.rpcUser" placeholder="bitcoinrpc" />
          </el-form-item>
          <el-form-item label="RPC 密码" prop="rpcPassword">
            <el-input
              v-model="form.rpcPassword"
              type="password"
              show-password
              :placeholder="editing?.rpcPasswordConfigured ? '留空表示不修改' : '请输入密码'"
            />
          </el-form-item>
        </template>

        <el-form-item label="确认数" prop="requiredConfirms">
          <el-input-number v-model="form.requiredConfirms" :min="1" :max="100" />
        </el-form-item>

        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ChainTag from '@/components/ChainTag.vue'
import {
  getChainNodeList,
  updateChainNode,
  getChainNodeApiKeys,
  addChainNodeApiKey,
  deleteChainNodeApiKey,
  type ChainNodeConfig,
  type ChainNodeApiKeyItem,
  type ChainNodeConfigUpdateReq,
} from '@/api/modules/chainNode'

const PROVIDER_LABELS: Record<string, string> = {
  ALCHEMY: 'Alchemy',
  INFURA: 'Infura',
  CUSTOM: '自定义 RPC',
  TRONGRID: 'TronGrid',
  BITCOIN_CORE: 'Bitcoin Core',
}

const EVM_PROVIDERS = [
  { value: 'ALCHEMY', label: 'Alchemy' },
  { value: 'INFURA', label: 'Infura' },
  { value: 'CUSTOM', label: '自定义 RPC' },
]

const loading = ref(false)
const saving = ref(false)
const addingKey = ref(false)
const deletingKeyId = ref<number | null>(null)
const tableData = ref<ChainNodeConfig[]>([])
const formVisible = ref(false)
const editing = ref<ChainNodeConfig | null>(null)
const formRef = ref<FormInstance>()
const apiKeyList = ref<ChainNodeApiKeyItem[]>([])
const newApiKey = ref('')
const newApiKeyLabel = ref('')

const form = reactive({
  provider: 'CUSTOM',
  rpcUrl: '',
  apiUrl: '',
  rpcUser: '',
  rpcPassword: '',
  requiredConfirms: 12,
  enabled: true,
  remark: '',
})

const providerOptions = computed(() => {
  const chain = editing.value?.chainCode
  if (chain === 'TRON') {
    return [{ value: 'TRONGRID', label: 'TronGrid' }]
  }
  if (chain === 'BTC') {
    return [{ value: 'BITCOIN_CORE', label: 'Bitcoin Core' }]
  }
  return EVM_PROVIDERS
})

const needsApiKeyForm = computed(() => {
  return form.provider === 'ALCHEMY' || form.provider === 'INFURA'
    || (editing.value?.chainCode === 'TRON' && form.provider === 'TRONGRID')
})

const needsRpcUrl = computed(() => {
  return form.provider === 'CUSTOM' || form.provider === 'BITCOIN_CORE'
})

const isTron = computed(() => editing.value?.chainCode === 'TRON')
const isBtc = computed(() => editing.value?.chainCode === 'BTC')

const formRules = computed<FormRules>(() => {
  const rules: FormRules = {
    provider: [{ required: true, message: '请选择服务商', trigger: 'change' }],
  }
  if (needsRpcUrl.value) {
    rules.rpcUrl = [{ required: true, message: '请填写 RPC 地址', trigger: 'blur' }]
  }
  if (isTron.value) {
    rules.apiUrl = [{ required: true, message: '请填写 API 地址', trigger: 'blur' }]
  }
  return rules
})

function providerLabel(code: string) {
  return PROVIDER_LABELS[code] || code
}

function needsApiKey(row: ChainNodeConfig) {
  return row.provider === 'ALCHEMY' || row.provider === 'INFURA' || row.chainCode === 'TRON'
}

function displayEndpoint(row: ChainNodeConfig) {
  if (row.chainCode === 'TRON') {
    return row.apiUrl || '—'
  }
  return row.effectiveRpcUrlMasked || row.rpcUrl || '—'
}

async function loadApiKeys(chainCode: string) {
  apiKeyList.value = await getChainNodeApiKeys(chainCode)
}

async function load() {
  loading.value = true
  try {
    tableData.value = await getChainNodeList()
  } finally {
    loading.value = false
  }
}

async function openEdit(row: ChainNodeConfig) {
  editing.value = row
  form.provider = row.provider
  form.rpcUrl = row.rpcUrl || ''
  form.apiUrl = row.apiUrl || ''
  form.rpcUser = row.rpcUser || ''
  form.rpcPassword = ''
  form.requiredConfirms = row.requiredConfirms ?? 12
  form.enabled = row.isEnabled === 1
  form.remark = row.remark || ''
  newApiKey.value = ''
  newApiKeyLabel.value = ''
  formVisible.value = true
  if (needsApiKey(row)) {
    await loadApiKeys(row.chainCode)
  } else {
    apiKeyList.value = []
  }
}

async function handleAddApiKey() {
  if (!editing.value) {
    return
  }
  if (!newApiKey.value.trim()) {
    ElMessage.warning('请输入 API Key')
    return
  }
  addingKey.value = true
  try {
    await addChainNodeApiKey(editing.value.chainCode, {
      apiKey: newApiKey.value.trim(),
      label: newApiKeyLabel.value.trim() || undefined,
    })
    ElMessage.success('API Key 已添加')
    newApiKey.value = ''
    newApiKeyLabel.value = ''
    await loadApiKeys(editing.value.chainCode)
    await load()
  } finally {
    addingKey.value = false
  }
}

async function handleDeleteApiKey(keyId: number) {
  if (!editing.value) {
    return
  }
  try {
    await ElMessageBox.confirm('确定删除该 API Key？', '提示', { type: 'warning' })
  } catch {
    return
  }
  deletingKeyId.value = keyId
  try {
    await deleteChainNodeApiKey(editing.value.chainCode, keyId)
    ElMessage.success('已删除')
    await loadApiKeys(editing.value.chainCode)
    await load()
  } finally {
    deletingKeyId.value = null
  }
}

async function handleSave() {
  if (needsApiKeyForm.value && apiKeyList.value.length === 0) {
    ElMessage.warning('请至少添加一个 API Key')
    return
  }

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !editing.value) {
    return
  }

  const payload: ChainNodeConfigUpdateReq = {
    provider: form.provider,
    rpcUrl: form.rpcUrl || undefined,
    apiUrl: form.apiUrl || undefined,
    rpcUser: form.rpcUser || undefined,
    requiredConfirms: form.requiredConfirms,
    isEnabled: form.enabled ? 1 : 0,
    remark: form.remark || undefined,
  }
  if (form.rpcPassword) {
    payload.rpcPassword = form.rpcPassword
  }

  saving.value = true
  try {
    await updateChainNode(editing.value.chainCode, payload)
    ElMessage.success('保存成功，Gateway 将自动刷新配置')
    formVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

watch(
  () => form.provider,
  async (provider) => {
    if (!formVisible.value || !editing.value) {
      return
    }
    const needs = provider === 'ALCHEMY' || provider === 'INFURA'
      || (editing.value.chainCode === 'TRON' && provider === 'TRONGRID')
    if (needs) {
      await loadApiKeys(editing.value.chainCode)
    } else {
      apiKeyList.value = []
    }
  },
)

onMounted(load)
</script>

<style scoped>
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}
.text-secondary {
  color: #909399;
}
.mb-4 {
  margin-bottom: 16px;
}
.api-key-panel {
  width: 100%;
}
.api-key-list {
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 8px 12px;
}
.api-key-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px dashed var(--el-border-color-extra-light);
}
.api-key-row:last-child {
  border-bottom: none;
}
.api-key-label {
  color: #909399;
  font-size: 12px;
}
.api-key-add {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.api-key-input {
  flex: 1;
  min-width: 160px;
}
.api-key-label-input {
  width: 120px;
}
.api-key-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #909399;
}
</style>
