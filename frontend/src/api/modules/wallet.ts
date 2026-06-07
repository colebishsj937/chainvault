import adminHttp from '@/api/adminHttp'
import type { SweepTriggerResult } from '@/api/types/sweep'

export interface WalletBalance {
  chainCode: string
  coinType: string
  symbol: string
  balance: string
  frozenBalance: string
}

export interface WalletCollectParams {
  chainCode: string
  merchantId: string
  coinType?: string
}

export function getWalletBalances(merchantId?: string) {
  return adminHttp.get<WalletBalance[]>('/wallets/balances', {
    params: merchantId ? { merchantId } : undefined,
  })
}

export function triggerCollection(params: WalletCollectParams) {
  const query: Record<string, string> = {
    merchantId: params.merchantId,
  }
  if (params.coinType) {
    query.coinType = params.coinType
  }
  return adminHttp.post<SweepTriggerResult>(`/wallets/${params.chainCode}/collect`, null, { params: query })
}
