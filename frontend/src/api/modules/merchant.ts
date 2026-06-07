import adminHttp from '@/api/adminHttp'
import type { PageResult } from '@/api/types/common'
import type { Merchant, MerchantCreateParams, ApiKeyResetResult } from '@/api/types/merchant'

export function getMerchantList(params: { page: number; size: number }) {
  return adminHttp.get<PageResult<Merchant>>('/merchants', { params })
}

export function getMerchant(merchantId: string) {
  return adminHttp.get<Merchant>(`/merchants/${merchantId}`)
}

export function createMerchant(data: MerchantCreateParams) {
  return adminHttp.post<Merchant>('/merchants', data)
}

/**
 * 更新商户状态（0=禁用, 1=正常, 2=冻结）
 */
export function updateMerchantStatus(merchantId: string, status: number) {
  return adminHttp.put(`/merchants/${merchantId}/status`, { status })
}

/**
 * 轮换 API 签名密钥，返回新 secretKey（仅此一次展示）
 */
export function rotateMerchantSecret(merchantId: string) {
  return adminHttp.post<ApiKeyResetResult>(`/merchants/${merchantId}/rotate-secret`)
}
