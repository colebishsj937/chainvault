import adminHttp from '@/api/adminHttp'
import type { PageResult } from '@/api/types/common'
import type { DepositRecord, DepositFilter, WithdrawRecord, AddressRecord } from '@/api/types/transaction'

export function getDepositList(params: DepositFilter) {
  return adminHttp.get<PageResult<DepositRecord>>('/transactions/deposits', { params })
}

export function getWithdrawList(params: DepositFilter) {
  return adminHttp.get<PageResult<WithdrawRecord>>('/transactions/withdraws', { params })
}

export function approveWithdraw(orderNo: string) {
  return adminHttp.post(`/transactions/withdraws/${orderNo}/approve`)
}

export function rejectWithdraw(orderNo: string, reason: string) {
  return adminHttp.post(`/transactions/withdraws/${orderNo}/reject`, { reason })
}
