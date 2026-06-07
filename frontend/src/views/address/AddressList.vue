<template>
  <div class="page-container">
    <PageHeader title="充值地址管理" />

    <el-card class="filter-card">
      <el-form :model="filters" inline>
        <el-form-item label="商户">
          <el-input v-model="filters.merchantId" clearable placeholder="商户ID" />
        </el-form-item>
        <el-form-item label="币种">
          <el-input v-model="filters.symbol" clearable placeholder="币种符号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load(1)">查询</el-button>
          <el-button @click="openBatchGenerate">批量生成</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column label="地址ID" prop="addressId" width="200" />
        <el-table-column label="商户" prop="merchantId" width="160" />
        <el-table-column label="链" prop="chainCode" width="80">
          <template #default="{ row }">
            <ChainTag :chain="row.chainCode" />
          </template>
        </el-table-column>
        <el-table-column label="币种" prop="symbol" width="80" />
        <el-table-column label="地址" min-width="200">
          <template #default="{ row }">
            <AddressCell :address="row.address" :chain="row.chainCode" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="180" />
      </el-table>

      <el-pagination class="mt-4" layout="total, sizes, prev, pager, next"
                     :total="total" :page-size="pageSize"
                     v-model:current-page="currentPage"
                     @change="load" />
    </el-card>

    <el-dialog v-model="generateVisible" title="批量生成地址" width="480px">
      <el-form :model="genForm" ref="genFormRef" :rules="genRules" label-width="100px">
        <el-form-item label="商户ID" prop="merchantId">
          <el-input v-model="genForm.merchantId" />
        </el-form-item>
        <el-form-item label="链" prop="chainCode">
          <el-select v-model="genForm.chainCode" @change="onChainChange">
            <el-option
              v-for="chain in SUPPORTED_ADDRESS_CHAINS"
              :key="chain"
              :label="chain"
              :value="chain"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="币种" prop="symbol">
          <el-select v-model="genForm.symbol" placeholder="请选择币种">
            <el-option
              v-for="symbol in availableSymbols"
              :key="symbol"
              :label="symbol"
              :value="symbol"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" prop="count">
          <el-input-number v-model="genForm.count" :min="1" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="handleGenerate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ChainTag from '@/components/ChainTag.vue'
import AddressCell from '@/components/AddressCell.vue'
import { getAddressList, batchGenerateAddresses } from '@/api/modules/address'
import { SUPPORTED_ADDRESS_CHAINS, SUPPORTED_COINS_BY_CHAIN } from '@/constants/coins'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

const filters = reactive({ merchantId: '', symbol: '' })

async function load(page = currentPage.value) {
  loading.value = true
  try {
    const res = await getAddressList({ page, size: pageSize.value, ...filters })
    tableData.value = res.records
    total.value = res.total
    currentPage.value = page
  } finally {
    loading.value = false
  }
}

const generateVisible = ref(false)
const generating = ref(false)
const genFormRef = ref<FormInstance>()
const genForm = reactive({ merchantId: '', chainCode: 'ETH', symbol: 'ETH', count: 1 })
const genRules = {
  merchantId: [{ required: true, message: '请输入商户ID', trigger: 'blur' }],
  chainCode: [{ required: true, message: '请选择链', trigger: 'change' }],
  symbol: [{ required: true, message: '请选择币种', trigger: 'change' }],
}

const availableSymbols = computed(() => {
  return SUPPORTED_COINS_BY_CHAIN[genForm.chainCode] ?? []
})

function resetSymbolForChain(chainCode: string) {
  const symbols = SUPPORTED_COINS_BY_CHAIN[chainCode] ?? []
  if (symbols.length === 0) {
    genForm.symbol = ''
    return
  }
  if (!symbols.includes(genForm.symbol)) {
    genForm.symbol = symbols[0]
  }
}

function onChainChange(chainCode: string) {
  resetSymbolForChain(chainCode)
}

function openBatchGenerate() {
  genForm.merchantId = ''
  genForm.chainCode = 'ETH'
  genForm.symbol = 'ETH'
  genForm.count = 1
  generateVisible.value = true
}

async function handleGenerate() {
  const valid = await genFormRef.value?.validate().catch(() => false)
  if (!valid) { return }
  generating.value = true
  try {
    await batchGenerateAddresses(genForm)
    ElMessage.success('生成成功')
    generateVisible.value = false
    load(1)
  } finally {
    generating.value = false
  }
}

onMounted(() => load())
</script>
