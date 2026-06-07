<template>
  <div class="page-container">
    <PageHeader title="报表导出" />

    <el-card>
      <el-form :model="form" inline>
        <el-form-item label="导出类型">
          <el-select v-model="form.type">
            <el-option label="充值记录" value="deposit" />
            <el-option label="提币记录" value="withdraw" />
            <el-option label="商户汇总" value="merchant_summary" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker v-model="form.dateRange" type="daterange"
                          range-separator="至" start-placeholder="开始"
                          end-placeholder="结束" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="商户">
          <el-input v-model="form.merchantId" clearable placeholder="全部商户" />
        </el-form-item>
        <el-form-item label="格式">
          <el-radio-group v-model="form.format">
            <el-radio value="xlsx">Excel (.xlsx)</el-radio>
            <el-radio value="csv">CSV</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Download" :loading="exporting" @click="handleExport">
            导出
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'

const exporting = ref(false)

const form = reactive({
  type: 'deposit',
  dateRange: [] as string[],
  merchantId: '',
  format: 'xlsx',
})

function handleExport() {
  exporting.value = true
  const params = new URLSearchParams({
    type: form.type,
    startDate: form.dateRange?.[0] ?? '',
    endDate: form.dateRange?.[1] ?? '',
    merchantId: form.merchantId,
    format: form.format,
  })
  window.open(`/api/v1/reports/export?${params.toString()}`)
  ElMessage.success('导出任务已创建')
  exporting.value = false
}
</script>
