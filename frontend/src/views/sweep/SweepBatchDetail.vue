<template>
  <div class="page-container">
    <PageHeader title="批次详情" :parent="{ label: '归集历史', to: '/sweep' }">
      <template #extra>
        <el-button
          v-if="batch.failedCount > 0"
          type="warning"
          :loading="retryingBatch"
          @click="handleRetryBatch"
        >
          重试全部失败
        </el-button>
      </template>
    </PageHeader>

    <el-card v-loading="batchLoading" class="mb-4">
      <template #header>批次信息</template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="批次号">
          <CopyButton :text="batch.batchNo">
            <span class="mono">{{ batch.batchNo }}</span>
          </CopyButton>
        </el-descriptions-item>
        <el-descriptions-item label="商户">{{ batch.merchantId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <SweepStatusTag :status="batch.status" type="batch" />
        </el-descriptions-item>
        <el-descriptions-item label="链">
          <ChainTag v-if="batch.chainCode" :chain="batch.chainCode" />
          <span v-else>—</span>
        </el-descriptions-item>
        <el-descriptions-item label="币种">{{ batch.coinType || '—' }}</el-descriptions-item>
        <el-descriptions-item label="触发方式">
          {{ SWEEP_TRIGGER_TYPE_MAP[batch.triggerType] ?? batch.triggerType }}
        </el-descriptions-item>
        <el-descriptions-item label="触发人">{{ batch.triggerBy || '—' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ batch.createdAt || '—' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ batch.completedAt || '—' }}</el-descriptions-item>
        <el-descriptions-item label="扫描 / 入队">
          {{ batch.scannedCount }} / {{ batch.queuedCount }}
        </el-descriptions-item>
        <el-descriptions-item label="成功 / 失败 / 跳过">
          <span class="text-success">{{ batch.successCount }}</span>
          /
          <span :class="{ 'text-danger': batch.failedCount > 0 }">{{ batch.failedCount }}</span>
          /
          {{ batch.skippedCount }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card>
      <template #header>
        <div class="records-header">
          <span>归集明细</span>
          <el-form :model="recordFilters" inline class="record-filters">
            <el-form-item label="状态">
              <el-select v-model="recordFilters.status" clearable placeholder="全部" style="width: 120px">
                <el-option
                  v-for="(label, value) in SWEEP_RECORD_STATUS_MAP"
                  :key="value"
                  :label="label"
                  :value="Number(value)"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="地址">
              <el-input
                v-model="recordFilters.fromAddress"
                clearable
                placeholder="来源地址"
                style="width: 220px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" @click="loadRecords(1)">筛选</el-button>
            </el-form-item>
          </el-form>
        </div>
      </template>

      <el-table :data="records" v-loading="recordsLoading" border stripe highlight-current-row>
        <el-table-column label="明细号" prop="recordNo" min-width="210" fixed>
          <template #default="{ row }">
            <el-button type="primary" link @click="openRecord(row.recordNo)">
              <span class="mono text-sm">{{ row.recordNo }}</span>
            </el-button>
          </template>
        </el-table-column>

        <el-table-column label="重试序" prop="retrySeq" width="70" align="center" />

        <el-table-column label="来源地址" min-width="160">
          <template #default="{ row }">
            <AddressCell :address="row.fromAddress" :chain="row.chainCode" />
          </template>
        </el-table-column>

        <el-table-column label="目标地址" min-width="160">
          <template #default="{ row }">
            <AddressCell v-if="row.toAddress" :address="row.toAddress" :chain="row.chainCode" />
            <span v-else>—</span>
          </template>
        </el-table-column>

        <el-table-column label="金额" width="150" align="right">
          <template #default="{ row }">
            <AmountDisplay :amount="row.amount" :symbol="row.coinType" />
          </template>
        </el-table-column>

        <el-table-column label="TxHash" width="160">
          <template #default="{ row }">
            <HashLink v-if="row.txHash" :hash="row.txHash" :chain="row.chainCode" />
            <span v-else>—</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <SweepStatusTag :status="row.status" type="record" />
          </template>
        </el-table-column>

        <el-table-column label="错误" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.errorMessage" class="text-danger">{{ row.errorMessage }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" prop="createdAt" width="170" />

        <el-table-column label="操作" width="90" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 5"
              type="warning"
              link
              :loading="retryingRecord === row.recordNo"
              @click="handleRetryRecord(row.recordNo)"
            >
              重试
            </el-button>
            <el-button v-else type="primary" link @click="openRecord(row.recordNo)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="mt-4"
        layout="total, sizes, prev, pager, next"
        :total="recordTotal"
        :page-sizes="[10, 20, 50]"
        v-model:current-page="recordPage"
        v-model:page-size="recordPageSize"
        @current-change="loadRecords"
        @size-change="() => loadRecords(1)"
      />
    </el-card>

    <el-drawer v-model="drawerVisible" title="明细详情" size="480px" destroy-on-close>
      <div v-loading="drawerLoading">
        <el-descriptions v-if="currentRecord" :column="1" border>
          <el-descriptions-item label="明细号">{{ currentRecord.recordNo }}</el-descriptions-item>
          <el-descriptions-item label="批次号">{{ currentRecord.batchNo }}</el-descriptions-item>
          <el-descriptions-item v-if="currentRecord.parentRecordNo" label="父明细">
            {{ currentRecord.parentRecordNo }}
          </el-descriptions-item>
          <el-descriptions-item label="重试序">{{ currentRecord.retrySeq }}</el-descriptions-item>
          <el-descriptions-item label="商户">{{ currentRecord.merchantId }}</el-descriptions-item>
          <el-descriptions-item label="链 / 币种">
            <ChainTag :chain="currentRecord.chainCode" />
            {{ currentRecord.coinType }}
          </el-descriptions-item>
          <el-descriptions-item label="来源地址">
            <AddressCell :address="currentRecord.fromAddress" :chain="currentRecord.chainCode" />
          </el-descriptions-item>
          <el-descriptions-item label="目标地址">
            <AddressCell
              v-if="currentRecord.toAddress"
              :address="currentRecord.toAddress"
              :chain="currentRecord.chainCode"
            />
            <span v-else>—</span>
          </el-descriptions-item>
          <el-descriptions-item label="归集金额">{{ currentRecord.amount }}</el-descriptions-item>
          <el-descriptions-item label="阈值快照">{{ currentRecord.thresholdSnapshot || '—' }}</el-descriptions-item>
          <el-descriptions-item label="待归集快照">{{ currentRecord.pendingSnapshot || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <SweepStatusTag :status="currentRecord.status" type="record" />
          </el-descriptions-item>
          <el-descriptions-item label="交易ID">{{ currentRecord.tradeId || '—' }}</el-descriptions-item>
          <el-descriptions-item label="TxHash">
            <HashLink
              v-if="currentRecord.txHash"
              :hash="currentRecord.txHash"
              :chain="currentRecord.chainCode"
            />
            <span v-else>—</span>
          </el-descriptions-item>
          <el-descriptions-item label="确认数">
            {{ currentRecord.confirms ?? 0 }} / {{ currentRecord.requiredConfirms ?? '—' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentRecord.errorCode" label="错误码">
            {{ currentRecord.errorCode }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentRecord.errorMessage" label="错误信息">
            <span class="text-danger">{{ currentRecord.errorMessage }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="入队时间">{{ currentRecord.queuedAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="广播时间">{{ currentRecord.broadcastAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="确认时间">{{ currentRecord.confirmedAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="失败时间">{{ currentRecord.failedAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentRecord.createdAt || '—' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button
          v-if="currentRecord?.status === 5"
          type="warning"
          :loading="retryingRecord === currentRecord?.recordNo"
          @click="handleRetryRecord(currentRecord!.recordNo)"
        >
          重试此明细
        </el-button>
        <el-button @click="drawerVisible = false">关闭</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import CopyButton from '@/components/CopyButton.vue'
import ChainTag from '@/components/ChainTag.vue'
import AddressCell from '@/components/AddressCell.vue'
import AmountDisplay from '@/components/AmountDisplay.vue'
import HashLink from '@/components/HashLink.vue'
import SweepStatusTag from '@/components/SweepStatusTag.vue'
import {
  getSweepBatch,
  getSweepRecordList,
  getSweepRecord,
  retrySweepBatchFailed,
  retrySweepRecord,
} from '@/api/modules/sweep'
import {
  SWEEP_TRIGGER_TYPE_MAP,
  SWEEP_RECORD_STATUS_MAP,
} from '@/constants/sweep'
import type { SweepBatch, SweepRecord } from '@/api/types/sweep'

const route = useRoute()
const router = useRouter()
const batchNo = ref(route.params.batchNo as string)

const batchLoading = ref(false)
const batch = ref<SweepBatch>({
  batchNo: '',
  triggerType: 0,
  status: 0,
  scannedCount: 0,
  queuedCount: 0,
  successCount: 0,
  failedCount: 0,
  skippedCount: 0,
})

const recordsLoading = ref(false)
const records = ref<SweepRecord[]>([])
const recordTotal = ref(0)
const recordPage = ref(1)
const recordPageSize = ref(20)

const recordFilters = reactive({
  status: null as number | null,
  fromAddress: '',
})

const drawerVisible = ref(false)
const drawerLoading = ref(false)
const currentRecord = ref<SweepRecord | null>(null)

const retryingBatch = ref(false)
const retryingRecord = ref<string | null>(null)

async function loadBatch() {
  batchLoading.value = true
  try {
    batch.value = await getSweepBatch(batchNo.value)
  } catch {
    router.replace('/sweep')
  } finally {
    batchLoading.value = false
  }
}

async function loadRecords(page = recordPage.value) {
  recordsLoading.value = true
  try {
    const res = await getSweepRecordList({
      page,
      size: recordPageSize.value,
      batchNo: batchNo.value,
      status: recordFilters.status,
      fromAddress: recordFilters.fromAddress || undefined,
    })
    records.value = res.records
    recordTotal.value = res.total
    recordPage.value = page
  } finally {
    recordsLoading.value = false
  }
}

async function openRecord(recordNo: string) {
  drawerVisible.value = true
  drawerLoading.value = true
  currentRecord.value = null
  try {
    currentRecord.value = await getSweepRecord(recordNo)
  } finally {
    drawerLoading.value = false
  }
}

async function handleRetryBatch() {
  try {
    await ElMessageBox.confirm('将为本批次所有失败明细创建重试任务，确认继续？', '重试失败明细', {
      type: 'warning',
    })
  } catch {
    return
  }
  retryingBatch.value = true
  try {
    const result = await retrySweepBatchFailed(batchNo.value)
    ElMessage.success(`已提交重试（入队 ${result.queued} 笔，跳过 ${result.skipped} 笔）`)
    if (result.batchNo) {
      router.push(`/sweep/${result.batchNo}`)
    } else {
      await loadBatch()
      await loadRecords(1)
    }
  } finally {
    retryingBatch.value = false
  }
}

async function handleRetryRecord(recordNo: string) {
  try {
    await ElMessageBox.confirm('将为该失败明细创建重试任务，确认继续？', '重试明细', {
      type: 'warning',
    })
  } catch {
    return
  }
  retryingRecord.value = recordNo
  try {
    const result = await retrySweepRecord(recordNo)
    ElMessage.success(`重试已提交（入队 ${result.queued} 笔）`)
    drawerVisible.value = false
    if (result.batchNo && result.batchNo !== batchNo.value) {
      router.push(`/sweep/${result.batchNo}`)
    } else {
      await loadBatch()
      await loadRecords(recordPage.value)
    }
  } finally {
    retryingRecord.value = null
  }
}

watch(
  () => route.params.batchNo,
  (val) => {
    if (typeof val === 'string' && val) {
      batchNo.value = val
      loadBatch()
      loadRecords(1)
    }
  },
)

onMounted(() => {
  loadBatch()
  loadRecords()
})
</script>

<style scoped>
.mb-4 {
  margin-bottom: 16px;
}
.mono {
  font-family: ui-monospace, monospace;
}
.text-sm {
  font-size: 13px;
}
.text-success {
  color: #67c23a;
  font-weight: 600;
}
.text-danger {
  color: #f56c6c;
}
.records-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.record-filters {
  margin: 0;
}
.record-filters :deep(.el-form-item) {
  margin-bottom: 0;
}
</style>
