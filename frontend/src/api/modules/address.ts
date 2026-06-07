import adminHttp from '@/api/adminHttp'
import type { PageResult } from '@/api/types/common'
import type { AddressRecord } from '@/api/types/transaction'

export function getAddressList(params: { page: number; size: number; merchantId?: string; symbol?: string }) {
  return adminHttp.get<PageResult<AddressRecord>>('/addresses', { params })
}

export function batchGenerateAddresses(data: { merchantId: string; chainCode: string; symbol: string; count: number }) {
  return adminHttp.post<AddressRecord[]>('/addresses/batch', data)
}
