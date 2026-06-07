<template>
  <div class="page-container">
    <PageHeader title="归集配置" :parent="{ label: '归集历史', to: '/sweep' }" />

    <el-alert
      class="mb-4"
      type="info"
      :closable="false"
      show-icon
      title="归集阈值 = 最小充值(min_deposit) × 阈值倍数。配置保存后立即生效，手动归集与定时扫描共用同一套阈值。"
    />

    <el-card class="mb-4" v-loading="configLoading">
      <template #header>全局配置</template>
      <el-form :model="configForm" label-width="140px" style="max-width: 520px">
        <el-form-item label="定时归集扫描">
          <el-switch
            v-model="configForm.sweepEnabled"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
        <el-form-item label="阈值倍数">
          <el-input-number
            v-model="configForm.thresholdMultiplier"
            :min="1"
            :max="100"
            :step="1"
            controls-position="right"
          />
          <div class="form-tip">有效阈值 = 各币种最小充值 × 倍数</div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingConfig" @click="handleSaveConfig">保存全局配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="tableLoading">
      <template #header>币种归集阈值</template>
      <el-table :data="tableData" border stripe>
        <el-table-column label="链" prop="chainCode" width="90">
          <template #default="{ row }">
            <ChainTag :chain="row.chainCode" />
          </template>
        </el-table-column>
        <el-table-column label="币种" prop="coinType" width="120" />
        <el-table-column label="符号" prop="symbol" width="80" />
        <el-table-column label="最小充值" prop="minDeposit" width="140" />
        <el-table-column label="归集阈值" prop="sweepThreshold" width="140" />
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled === 1 ? 'success' : 'info'" size="small">
              {{ row.isEnabled === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editVisible" :title="`编辑 ${editing?.coinType} 阈值基数`" width="480px">
      <el-form :model="editForm" ref="editFormRef" :rules="editRules" label-width="120px">
        <el-form-item label="最小充值" prop="minDeposit">
          <el-input v-model="editForm.minDeposit" placeholder="如 1 或 0.001" />
        </el-form-item>
        <el-form-item label="当前倍数">
          <span>{{ configForm.thresholdMultiplier }}</span>
        </el-form-item>
        <el-form-item label="预计阈值">
          <span class="preview-threshold">{{ previewThreshold }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingCoin" @click="handleSaveCoin">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ChainTag from '@/components/ChainTag.vue'
import {
  getSweepConfig,
  updateSweepConfig,
  getSweepCoinThresholds,
  updateSweepCoinThreshold,
} from '@/api/modules/sweep'
import type { SweepCoinThreshold } from '@/api/types/sweep'

const configLoading = ref(false)
const tableLoading = ref(false)
const savingConfig = ref(false)
const savingCoin = ref(false)
const editVisible = ref(false)
const editFormRef = ref<FormInstance>()
const editing = ref<SweepCoinThreshold | null>(null)
const tableData = ref<SweepCoinThreshold[]>([])

const configForm = reactive({
  sweepEnabled: 1,
  thresholdMultiplier: 5,
})

const editForm = reactive({
  minDeposit: '',
})

const editRules: FormRules = {
  minDeposit: [
    { required: true, message: '请输入最小充值金额', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const num = Number(value)
        if (Number.isNaN(num) || num <= 0) {
          callback(new Error('最小充值金额必须大于 0'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

const previewThreshold = computed(() => {
  const base = Number(editForm.minDeposit)
  if (Number.isNaN(base) || base <= 0) {
    return '—'
  }
  const result = base * configForm.thresholdMultiplier
  return String(result)
})

async function loadConfig() {
  configLoading.value = true
  try {
    const config = await getSweepConfig()
    configForm.sweepEnabled = config.sweepEnabled
    configForm.thresholdMultiplier = config.thresholdMultiplier
  } finally {
    configLoading.value = false
  }
}

async function loadCoinThresholds() {
  tableLoading.value = true
  try {
    tableData.value = await getSweepCoinThresholds()
  } finally {
    tableLoading.value = false
  }
}

async function handleSaveConfig() {
  savingConfig.value = true
  try {
    await updateSweepConfig({
      sweepEnabled: configForm.sweepEnabled,
      thresholdMultiplier: configForm.thresholdMultiplier,
    })
    ElMessage.success('全局配置已保存')
    await loadCoinThresholds()
  } finally {
    savingConfig.value = false
  }
}

function openEdit(row: SweepCoinThreshold) {
  editing.value = row
  editForm.minDeposit = row.minDeposit
  editVisible.value = true
}

async function handleSaveCoin() {
  if (!editing.value) {
    return
  }
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  savingCoin.value = true
  try {
    await updateSweepCoinThreshold(editing.value.coinType, {
      minDeposit: editForm.minDeposit,
    })
    ElMessage.success('币种阈值已更新')
    editVisible.value = false
    await loadCoinThresholds()
  } finally {
    savingCoin.value = false
  }
}

onMounted(async () => {
  await loadConfig()
  await loadCoinThresholds()
})
</script>

<style scoped>
.form-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}
.preview-threshold {
  font-weight: 600;
  color: #409eff;
}
</style>
