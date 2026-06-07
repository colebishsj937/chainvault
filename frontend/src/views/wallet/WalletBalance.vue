<template>
  <div class="page-container">
    <PageHeader title="热钱包余额" />

    <el-card class="filter-card">
      <el-form :model="filters" inline>
        <el-form-item label="商户" required>
          <el-select
            v-model="filters.merchantId"
            clearable
            filterable
            placeholder="请选择商户"
            style="width: 280px"
            :loading="merchantLoading"
          >
            <el-option
              v-for="item in merchantOptions"
              :key="item.merchantId"
              :label="`${item.merchantName}（${item.merchantId}）`"
              :value="item.merchantId"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-empty v-if="!filters.merchantId" description="请先选择商户并查询余额" />

    <el-row v-else :gutter="16" v-loading="loading">
      <el-col :span="8" v-for="b in balances" :key="b.coinType">
        <el-card class="balance-card">
          <div class="balance-header">
            <ChainTag :chain="b.chainCode" />
            <span class="symbol-text">{{ b.symbol }}</span>
          </div>
          <div class="balance-body">
            <div class="balance-item">
              <span class="label">可用余额</span>
              <span class="value">{{ b.balance }}</span>
            </div>
            <div class="balance-item">
              <span class="label">冻结余额</span>
              <span class="value text-warning">{{ b.frozenBalance }}</span>
            </div>
          </div>
          <div class="balance-footer">
            <el-button
              size="small"
              @click="doCollect(b)"
              :loading="collectingCoin === b.coinType"
            >
              归集
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ChainTag from '@/components/ChainTag.vue'
import { getMerchantList } from '@/api/modules/merchant'
import { getWalletBalances, triggerCollection } from '@/api/modules/wallet'
import type { Merchant } from '@/api/types/merchant'
import type { WalletBalance } from '@/api/modules/wallet'

const router = useRouter()
const loading = ref(false)
const merchantLoading = ref(false)
const collectingCoin = ref<string | null>(null)
const balances = ref<WalletBalance[]>([])
const merchantOptions = ref<Merchant[]>([])
const filters = reactive({ merchantId: '' })

async function loadMerchants() {
  merchantLoading.value = true
  try {
    const res = await getMerchantList({ page: 1, size: 500 })
    merchantOptions.value = res.records
  } finally {
    merchantLoading.value = false
  }
}

async function load() {
  if (!filters.merchantId) {
    ElMessage.warning('请先选择商户')
    return
  }
  loading.value = true
  try {
    balances.value = await getWalletBalances(filters.merchantId)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.merchantId = ''
  balances.value = []
}

async function doCollect(balance: WalletBalance) {
  const merchantId = filters.merchantId
  if (!merchantId) {
    ElMessage.warning('请先选择商户')
    return
  }
  collectingCoin.value = balance.coinType
  try {
    const result = await triggerCollection({
      chainCode: balance.chainCode,
      merchantId,
      coinType: balance.coinType,
    })
    const skippedText = result.skipped > 0 ? `，跳过 ${result.skipped} 笔` : ''
    ElMessage.success({
      message: `归集已提交（扫描 ${result.scanned}，入队 ${result.queued}${skippedText}）`,
      duration: 5000,
    })
    if (result.batchNo) {
      router.push(`/sweep/${result.batchNo}`)
    }
  } finally {
    collectingCoin.value = null
  }
}

onMounted(loadMerchants)
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}
.balance-card {
  margin-bottom: 16px;
}
.balance-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.symbol-text {
  font-weight: bold;
  font-size: 16px;
}
.balance-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}
.balance-item {
  display: flex;
  justify-content: space-between;
}
.label {
  color: #909399;
}
.value {
  font-weight: 600;
  font-size: 16px;
}
.text-warning {
  color: #e6a23c;
}
</style>
