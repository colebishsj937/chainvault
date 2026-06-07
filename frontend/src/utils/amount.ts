import BigNumber from 'bignumber.js'

BigNumber.config({
  EXPONENTIAL_AT: [-20, 20],
  ROUNDING_MODE: BigNumber.ROUND_DOWN,
})

/**
 * 将链上原始整数金额转为可读小数
 * rawAmount: "10500000" (USDT 6位) → "10.5"
 */
export function fromRaw(rawAmount: string, decimals: number): string {
  return new BigNumber(rawAmount).shiftedBy(-decimals).toFixed()
}

/**
 * 可读金额转链上原始整数
 */
export function toRaw(amount: string, decimals: number): string {
  return new BigNumber(amount).shiftedBy(decimals).integerValue().toFixed()
}

/**
 * 格式化展示金额，保留指定小数位，千分位分隔
 * 1000000.123456 → "1,000,000.1234"
 */
export function formatAmount(amount: string, displayDecimals = 4): string {
  const bn = new BigNumber(amount)
  if (bn.isNaN()) {
    return '—'
  }
  return bn.toFormat(displayDecimals)
}
