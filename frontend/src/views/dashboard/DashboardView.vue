<template>
  <div class="dashboard">
    <PageHeader title="数据总览" />

    <el-row :gutter="16" class="mb-4">
      <el-col :span="6" v-for="card in summaryCards" :key="card.label">
        <el-card class="summary-card">
          <div class="card-label">{{ card.label }}</div>
          <div class="card-value">{{ card.value }}</div>
          <div class="card-sub" :class="card.trend > 0 ? 'text-success' : 'text-danger'">
            {{ card.trend > 0 ? '↑' : '↓' }} {{ Math.abs(card.trend) }}% 较昨日
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="mb-4">
      <template #header>近7日充提趋势</template>
      <v-chart :option="lineChartOption" style="height:280px" autoresize />
    </el-card>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header>热钱包余额分布</template>
          <v-chart :option="pieChartOption" style="height:240px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>最近充值记录</template>
          <el-table :data="recentDeposits" size="small">
            <el-table-column label="币种" prop="symbol" width="80" />
            <el-table-column label="金额" width="120">
              <template #default="{ row }">
                <AmountDisplay :amount="row.amount" :symbol="row.symbol" />
              </template>
            </el-table-column>
            <el-table-column label="确认数" prop="confirms" width="80" />
            <el-table-column label="时间" prop="createdAt" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import PageHeader from '@/components/PageHeader.vue'
import AmountDisplay from '@/components/AmountDisplay.vue'
import { getDashboardSummary } from '@/api/modules/report'

use([LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const summary = ref<any>({})
const recentDeposits = ref<any[]>([])

const summaryCards = computed(() => [
  { label: '今日充值笔数', value: summary.value.todayDepositCount ?? 0, trend: 12 },
  { label: '今日充值金额', value: `$${summary.value.todayDepositAmount ?? 0}`, trend: 8 },
  { label: '今日提币笔数', value: summary.value.todayWithdrawCount ?? 0, trend: -3 },
  { label: '热钱包总余额', value: `$${summary.value.totalBalance ?? 0}`, trend: 5 },
])

const lineChartOption = computed(() => ({
  tooltip: { trigger: 'axis' as const },
  legend: { data: ['充值金额', '提币金额'] },
  xAxis: { type: 'category' as const, data: summary.value.dates ?? [] },
  yAxis: { type: 'value' as const },
  series: [
    { name: '充值金额', type: 'line' as const, smooth: true, data: summary.value.depositAmounts ?? [] },
    { name: '提币金额', type: 'line' as const, smooth: true, data: summary.value.withdrawAmounts ?? [] },
  ],
}))

const pieChartOption = computed(() => ({
  tooltip: { trigger: 'item' as const },
  legend: { orient: 'vertical' as const, left: 'left' },
  series: [{
    type: 'pie' as const,
    radius: '60%',
    data: (summary.value.balanceDistribution ?? []).map((b: any) => ({
      name: b.symbol,
      value: parseFloat(b.amount),
    })),
  }],
}))

onMounted(async () => {
  summary.value = await getDashboardSummary()
  recentDeposits.value = summary.value.recentDeposits ?? []
})
</script>

<style scoped>
.summary-card {
  text-align: center;
}
.card-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.card-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 8px;
}
.card-sub {
  font-size: 12px;
}
.text-success {
  color: #67c23a;
}
.text-danger {
  color: #f56c6c;
}
</style>
