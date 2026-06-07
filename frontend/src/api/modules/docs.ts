import adminHttp from '@/api/adminHttp'

export interface MerchantDocs {
  title: string
  version: string
  updatedAt: string
  gatewayBaseUrl: string
  markdown: string
}

/**
 * 获取商户 API 对接文档
 */
export function getMerchantDocs() {
  return adminHttp.get<MerchantDocs>('/docs/merchant')
}
