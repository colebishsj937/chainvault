export const CHAIN_EXPLORER: Record<string, { name: string; addressUrl: string; txUrl: string }> = {
  ETH: { name: 'Ethereum', addressUrl: 'https://etherscan.io/address/{0}', txUrl: 'https://etherscan.io/tx/{0}' },
  BTC: { name: 'Bitcoin', addressUrl: 'https://mempool.space/address/{0}', txUrl: 'https://mempool.space/tx/{0}' },
  TRON: { name: 'TRON', addressUrl: 'https://tronscan.org/#/address/{0}', txUrl: 'https://tronscan.org/#/transaction/{0}' },
  BNB: { name: 'BNB Chain', addressUrl: 'https://bscscan.com/address/{0}', txUrl: 'https://bscscan.com/tx/{0}' },
  MATIC: { name: 'Polygon', addressUrl: 'https://polygonscan.com/address/{0}', txUrl: 'https://polygonscan.com/tx/{0}' },
}

export function getExplorerAddressUrl(chain: string, address: string): string {
  return CHAIN_EXPLORER[chain]?.addressUrl.replace('{0}', address) ?? ''
}

export function getExplorerTxUrl(chain: string, hash: string): string {
  return CHAIN_EXPLORER[chain]?.txUrl.replace('{0}', hash) ?? ''
}
