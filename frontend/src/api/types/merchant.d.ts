export interface Merchant {
  merchantId: string
  merchantName: string
  apiKey: string
  callbackUrl?: string
  tier: number
  status: number
  createdAt: string
}

export interface MerchantCreateParams {
  merchantName: string
  callbackUrl?: string
  ipWhitelist?: string
  tier: number
}

/**
 * rotate-secret 接口返回
 */
export interface ApiKeyResetResult {
  secretKey: string
}
