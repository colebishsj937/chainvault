import adminHttp from '@/api/adminHttp'

export interface DashboardSummary {
  todayDepositCount: number
  todayDepositAmount: number
  todayWithdrawCount: number
  totalBalance: number
  dates: string[]
  depositAmounts: number[]
  withdrawAmounts: number[]
  balanceDistribution: { symbol: string; amount: string }[]
  recentDeposits: any[]
}

export function getDashboardSummary() {
  return adminHttp.get<DashboardSummary>('/reports/dashboard')
}
