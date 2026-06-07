<template>
  <div class="page-container">
    <PageHeader title="归集历史" />

    <el-card class="filter-card">
      <el-form :model="filters" inline>
        <el-form-item label="商户">
          <el-input v-model="filters.merchantId" clearable placeholder="商户ID" style="width: 160px" />
        </el-form-item>
        <el-form-item label="链">
          <el-input v-model="filters.chainCode" clearable placeholder="如 ETH" style="width: 100px" />
        </el-form-item>
        <el-form-item label="币种">
          <el-input v-model="filters.coinType" clearable placeholder="如 USDT_ETH" style="width: 140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部" style="width: 130px">
            <el-option
              v-for="(label, value) in SWEEP_BATCH_STATUS_MAP"
              :key="value"
              :label="label"
              :value="Number(value)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load(1)">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="tableData" v-loading="loading" border stripe highlight-current-row>
        <el-table-column label="批次号" prop="batchNo" min-width="220" fixed>
          <template #default="{ row }">
            <el-button type="primary" link @click="goDetail(row.batchNo)">
              <span class="mono text-sm">{{ row.batchNo }}</span>
            </el-button>
          </template>
        </el-table-column>

        <el-table-column label="商户" prop="merchantId" width="100" />

        <el-table-column label="链 / 币种" width="150">
          <template #default="{ row }">
            <ChainTag v-if="row.chainCode" :chain="row.chainCode" />
            <span v-if="row.coinType" class="ml-1 text-sm">{{ row.coinType }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>

        <el-table-column label="触发方式" width="110">
          <template #default="{ row }">
            {{ SWEEP_TRIGGER_TYPE_MAP[row.triggerType] ?? row.triggerType }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <SweepStatusTag :status="row.status" type="batch" />
          </template>
        </el-table-column>

        <el-table-column label="扫描" prop="scannedCount" width="70" align="center" />
        <el-table-column label="入队" prop="queuedCount" width="70" align="center" />
        <el-table-column label="成功" prop="successCount" width="70" align="center" />
        <el-table-column label="失败" prop="failedCount" width="70" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.failedCount > 0 }">{{ row.failedCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="跳过" prop="skippedCount" width="70" align="center" />

        <el-table-column label="创建时间" prop="createdAt" width="170" />
        <el-table-column label="完成时间" prop="completedAt" width="170" />

        <el-table-column label="操作" width="90" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="goDetail(row.batchNo)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="mt-4"
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50]"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        @current-change="load"
        @size-change="() => load(1)"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import ChainTag from '@/components/ChainTag.vue'
import SweepStatusTag from '@/components/SweepStatusTag.vue'
import { getSweepBatchList } from '@/api/modules/sweep'
import { SWEEP_BATCH_STATUS_MAP, SWEEP_TRIGGER_TYPE_MAP } from '@/constants/sweep'
import type { SweepBatch } from '@/api/types/sweep'

const router = useRouter()
const loading = ref(false)
const tableData = ref<SweepBatch[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

const filters = reactive({
  merchantId: '',
  chainCode: '',
  coinType: '',
  status: null as number | null,
  dateRange: [] as string[],
})

async function load(page = currentPage.value) {
  loading.value = true
  try {
    const res = await getSweepBatchList({
      page,
      size: pageSize.value,
      merchantId: filters.merchantId || undefined,
      chainCode: filters.chainCode || undefined,
      coinType: filters.coinType || undefined,
      status: filters.status,
      startDate: filters.dateRange?.[0],
      endDate: filters.dateRange?.[1],
    })
    tableData.value = res.records
    total.value = res.total
    currentPage.value = page
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.assign(filters, {
    merchantId: '',
    chainCode: '',
    coinType: '',
    status: null,
    dateRange: [],
  })
  load(1)
}

function goDetail(batchNo: string) {
  router.push(`/sweep/${batchNo}`)
}

onMounted(() => load())
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}
.mono {
  font-family: ui-monospace, monospace;
}
.text-sm {
  font-size: 13px;
}
.text-muted {
  color: #909399;
}
.text-danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>
