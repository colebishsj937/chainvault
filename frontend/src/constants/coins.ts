/**
 * 各链支持的显示符号（与后端 coin_config 种子数据一致）
 */
export const SUPPORTED_COINS_BY_CHAIN: Record<string, string[]> = {
  ETH: ['ETH', 'USDT'],
  BNB: ['BNB'],
  TRON: ['TRX', 'USDT'],
  BTC: ['BTC'],
}

/** 支持批量生成地址的链列表 */
export const SUPPORTED_ADDRESS_CHAINS = Object.keys(SUPPORTED_COINS_BY_CHAIN)
