/**
 * 交易状态（transaction_record.status）
 */
export const TX_STATUS_MAP: Record<number, string> = {
  0: '待处理',
  1: '处理中',
  2: '成功',
  3: '失败',
  4: '已回调',
}

export const TX_STATUS_COLORS: Record<number, string> = {
  0: 'info',
  1: 'warning',
  2: 'success',
  3: 'danger',
  4: '',
}

/**
 * 提币单状态（withdraw_order.status）
 */
export const WITHDRAW_STATUS_MAP: Record<number, string> = {
  0: '待审核',
  1: '审核通过',
  2: '广播中',
  3: '成功',
  4: '失败',
  5: '拒绝',
}

export const WITHDRAW_STATUS_COLORS: Record<number, string> = {
  0: 'info',
  1: 'warning',
  2: 'warning',
  3: 'success',
  4: 'danger',
  5: 'info',
}

/**
 * 商户状态
 */
export const MERCHANT_STATUS_MAP: Record<number, string> = {
  0: '禁用',
  1: '正常',
  2: '冻结',
}
