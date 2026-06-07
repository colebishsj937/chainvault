import adminHttp from '@/api/adminHttp'
import type { PageResult } from '@/api/types/common'
import type {
  SweepAddressSummary,
  SweepBatch,
  SweepBatchFilter,
  SweepCoinThreshold,
  SweepCoinThresholdUpdateParams,
  SweepConfig,
  SweepConfigUpdateParams,
  SweepRecord,
  SweepRecordFilter,
  SweepTriggerResult,
} from '@/api/types/sweep'

export function getSweepConfig() {
  return adminHttp.get<SweepConfig>('/sweeps/config')
}

export function updateSweepConfig(data: SweepConfigUpdateParams) {
  return adminHttp.put<SweepConfig>('/sweeps/config', data)
}

export function getSweepCoinThresholds() {
  return adminHttp.get<SweepCoinThreshold[]>('/sweeps/coin-thresholds')
}

export function updateSweepCoinThreshold(coinType: string, data: SweepCoinThresholdUpdateParams) {
  return adminHttp.put<SweepCoinThreshold>(`/sweeps/coin-thresholds/${coinType}`, data)
}

export function getSweepBatchList(params: SweepBatchFilter) {
  return adminHttp.get<PageResult<SweepBatch>>('/sweeps/batches', { params })
}

export function getSweepBatch(batchNo: string) {
  return adminHttp.get<SweepBatch>(`/sweeps/batches/${batchNo}`)
}

export function getSweepRecordList(params: SweepRecordFilter) {
  return adminHttp.get<PageResult<SweepRecord>>('/sweeps/records', { params })
}

export function getSweepRecord(recordNo: string) {
  return adminHttp.get<SweepRecord>(`/sweeps/records/${recordNo}`)
}

export function retrySweepRecord(recordNo: string) {
  return adminHttp.post<SweepTriggerResult>(`/sweeps/records/${recordNo}/retry`)
}

export function retrySweepBatchFailed(batchNo: string) {
  return adminHttp.post<SweepTriggerResult>(`/sweeps/batches/${batchNo}/retry-failed`)
}

export function getSweepAddressSummary(chainCode: string, address: string) {
  return adminHttp.get<SweepAddressSummary>(`/sweeps/addresses/${chainCode}/${address}/summary`)
}

export function getSweepAddressRecords(chainCode: string, address: string, page: number, size: number) {
  return adminHttp.get<PageResult<SweepRecord>>(`/sweeps/addresses/${chainCode}/${address}/records`, {
    params: { page, size },
  })
}
