export interface DepositRecord {
  tradeId: string
  merchantId: string
  chainCode: string
  symbol: string
  amount: string
  decimals: number
  toAddress: string
  txHash: string
  confirms: number
  requiredConfirms: number
  status: number
  createdAt: string
}

export interface WithdrawRecord {
  orderNo: string
  merchantId: string
  chainCode: string
  symbol: string
  amount: string
  fromAddress: string
  toAddress: string
  txHash: string
  fee: string
  status: number
  approvals: number
  requiredApprovals: number
  createdAt: string
}

export interface DepositFilter {
  page: number
  size: number
  merchantId?: string
  coinType?: string
  status?: number | null
  startDate?: string
  endDate?: string
}

export interface AddressRecord {
  addressId: string
  merchantId: string
  chainCode: string
  symbol: string
  address: string
  createdAt: string
}
