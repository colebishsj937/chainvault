<template>
  <div class="page-container">
    <PageHeader title="充值记录" />

    <el-card class="filter-card">
      <el-form :model="filters" inline>
        <el-form-item label="商户">
          <el-input v-model="filters.merchantId" clearable placeholder="商户ID" />
        </el-form-item>
        <el-form-item label="币种">
          <el-input v-model="filters.coinType" clearable placeholder="币种" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable>
            <el-option label="待处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="成功" :value="2" />
            <el-option label="失败" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="filters.dateRange" type="daterange"
                          range-separator="至" start-placeholder="开始日期"
                          end-placeholder="结束日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load(1)">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <el-button :icon="Download" @click="exportData">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="tableData" v-loading="loading" border stripe highlight-current-row>
        <el-table-column label="交易ID" prop="tradeId" width="200" fixed>
          <template #default="{ row }">
            <CopyButton :text="row.tradeId">
              <span class="mono text-sm">{{ row.tradeId }}</span>
            </CopyButton>
          </template>
        </el-table-column>

        <el-table-column label="链 / 币种" width="120">
          <template #default="{ row }">
            <ChainTag :chain="row.chainCode" />
            <span class="ml-1 text-sm">{{ row.symbol }}</span>
          </template>
        </el-table-column>

        <el-table-column label="金额" width="140" align="right">
          <template #default="{ row }">
            <AmountDisplay :amount="row.amount" :symbol="row.symbol" />
          </template>
        </el-table-column>

        <el-table-column label="目标地址" width="160">
          <template #default="{ row }">
            <AddressCell :address="row.toAddress" :chain="row.chainCode" />
          </template>
        </el-table-column>

        <el-table-column label="TxHash" width="160">
          <template #default="{ row }">
            <HashLink :hash="row.txHash" :chain="row.chainCode" />
          </template>
        </el-table-column>

        <el-table-column label="确认数" width="120" align="center">
          <template #default="{ row }">
            <template v-if="row.status === 1">
              <el-progress
                :percentage="Math.min(100, (row.confirms / row.requiredConfirms) * 100)"
                :format="() => `${row.confirms}/${row.requiredConfirms}`"
                :stroke-width="8" />
            </template>
            <span v-else>{{ row.confirms }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <TxStatusTag :status="row.status" />
          </template>
        </el-table-column>

        <el-table-column label="时间" width="160" prop="createdAt" />
      </el-table>

      <el-pagination class="mt-4" layout="total, sizes, prev, pager, next"
                     :total="total" :page-size="pageSize"
                     v-model:current-page="currentPage"
                     @change="load" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import CopyButton from '@/components/CopyButton.vue'
import ChainTag from '@/components/ChainTag.vue'
import AmountDisplay from '@/components/AmountDisplay.vue'
import AddressCell from '@/components/AddressCell.vue'
import HashLink from '@/components/HashLink.vue'
import TxStatusTag from '@/components/TxStatusTag.vue'
import { getDepositList } from '@/api/modules/transaction'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

const filters = reactive({
  merchantId: '',
  coinType: '',
  status: null as number | null,
  dateRange: [] as string[],
})

async function load(page = currentPage.value) {
  loading.value = true
  try {
    const res = await getDepositList({
      page,
      size: pageSize.value,
      merchantId: filters.merchantId,
      coinType: filters.coinType,
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
  Object.assign(filters, { merchantId: '', coinType: '', status: null, dateRange: [] })
  load(1)
}

function exportData() {
  window.open(`/api/v1/reports/export?type=deposit&merchantId=${filters.merchantId}`)
}

onMounted(() => load())
</script>
