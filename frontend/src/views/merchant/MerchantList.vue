<template>
  <div class="page-container">
    <PageHeader title="商户管理" />
    <el-card>
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增商户</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column label="商户ID" prop="merchantId" width="180" />
        <el-table-column label="商户名称" prop="merchantName" width="160" />
        <el-table-column label="版本" width="100">
          <template #default="{ row }">
            <el-tag :type="row.tier === 1 ? 'warning' : 'info'" size="small">
              {{ row.tier === 1 ? '商业版' : '开源版' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="180" />
        <el-table-column label="操作" min-width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/merchant/${row.merchantId}`)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="mt-4" layout="total, sizes, prev, pager, next"
                     :total="total" :page-size="pageSize"
                     v-model:current-page="currentPage"
                     @change="load" />
    </el-card>

    <el-dialog v-model="createVisible" title="新增商户" width="480px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px">
        <el-form-item label="商户名称" prop="merchantName">
          <el-input v-model="createForm.merchantName" placeholder="请输入商户名称" />
        </el-form-item>
        <el-form-item label="版本" prop="tier">
          <el-radio-group v-model="createForm.tier">
            <el-radio :value="0">开源版</el-radio>
            <el-radio :value="1">商业版</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { getMerchantList, createMerchant } from '@/api/modules/merchant'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

async function load(page = currentPage.value) {
  loading.value = true
  try {
    const res = await getMerchantList({ page, size: pageSize.value })
    tableData.value = res.records
    total.value = res.total
    currentPage.value = page
  } finally {
    loading.value = false
  }
}

const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ merchantName: '', tier: 1 })
const createRules = {
  merchantName: [{ required: true, message: '请输入商户名称', trigger: 'blur' }],
}

function openCreate() {
  createForm.merchantName = ''
  createForm.tier = 1
  createVisible.value = true
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) { return }
  creating.value = true
  try {
    await createMerchant(createForm)
    ElMessage.success('创建成功')
    createVisible.value = false
    load(1)
  } finally {
    creating.value = false
  }
}

onMounted(() => load())
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
