export interface SweepBatch {
  batchNo: string
  merchantId?: string | null
  chainCode?: string | null
  coinType?: string | null
  triggerType: number
  triggerBy?: string | null
  status: number
  statusLabel?: string
  scannedCount: number
  queuedCount: number
  successCount: number
  failedCount: number
  skippedCount: number
  createdAt?: string
  completedAt?: string
}

export interface SweepRecord {
  recordNo: string
  batchNo?: string
  parentRecordNo?: string | null
  retrySeq: number
  merchantId: string
  coinType: string
  chainCode: string
  fromAddress: string
  toAddress?: string | null
  amount: string
  thresholdSnapshot?: string
  pendingSnapshot?: string
  status: number
  statusLabel?: string
  tradeId?: string | null
  txHash?: string | null
  blockNumber?: number | null
  confirms?: number
  requiredConfirms?: number
  errorCode?: string | null
  errorMessage?: string | null
  queuedAt?: string | null
  broadcastAt?: string | null
  confirmedAt?: string | null
  failedAt?: string | null
  createdAt?: string
}

export interface SweepAddressSummary {
  merchantId: string
  coinType: string
  chainCode: string
  address: string
  totalDeposits: string
  alreadySwept: string
  pendingAmount: string
  lastStatus?: number | null
  lastStatusLabel?: string | null
  lastRecordNo?: string | null
}

export interface SweepTriggerResult {
  scanned: number
  queued: number
  skipped: number
  batchNo: string
}

export interface SweepBatchFilter {
  page?: number
  size?: number
  merchantId?: string
  chainCode?: string
  coinType?: string
  status?: number | null
  startDate?: string
  endDate?: string
}

export interface SweepConfig {
  sweepEnabled: number
  thresholdMultiplier: number
  thresholdFormula: string
  updatedAt?: string
}

export interface SweepConfigUpdateParams {
  sweepEnabled: number
  thresholdMultiplier: number
}

export interface SweepCoinThreshold {
  coinType: string
  symbol: string
  chainCode: string
  minDeposit: string
  sweepThreshold: string
  isEnabled: number
}

export interface SweepCoinThresholdUpdateParams {
  minDeposit: string
}

export interface SweepRecordFilter {
  page?: number
  size?: number
  batchNo?: string
  merchantId?: string
  chainCode?: string
  coinType?: string
  fromAddress?: string
  status?: number | null
}
