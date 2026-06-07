/**
 * 归集批次状态（sweep_batch.status）
 */
export const SWEEP_BATCH_STATUS_MAP: Record<number, string> = {
  0: '已创建',
  1: '执行中',
  2: '完成',
  3: '部分失败',
  4: '全部失败',
  5: '已取消',
}

export const SWEEP_BATCH_STATUS_COLORS: Record<number, string> = {
  0: 'info',
  1: 'warning',
  2: 'success',
  3: 'warning',
  4: 'danger',
  5: 'info',
}

/**
 * 归集明细状态（sweep_record.status）
 */
export const SWEEP_RECORD_STATUS_MAP: Record<number, string> = {
  1: '已入队',
  2: '广播中',
  3: '确认中',
  4: '成功',
  5: '失败',
  6: '跳过',
}

export const SWEEP_RECORD_STATUS_COLORS: Record<number, string> = {
  1: 'info',
  2: 'warning',
  3: 'warning',
  4: 'success',
  5: 'danger',
  6: 'info',
}

/**
 * 归集触发方式（sweep_batch.trigger_type）
 */
export const SWEEP_TRIGGER_TYPE_MAP: Record<number, string> = {
  1: '定时扫描',
  2: 'Admin手动',
  3: 'Admin批次重试',
  4: '商户API',
}
