import adminHttp from '@/api/adminHttp'

export interface ChainNodeApiKeyItem {
  id: number
  apiKeyMasked: string
  label?: string
  isEnabled: number
  sortOrder?: number
  createdAt?: string
}

export interface ChainNodeConfig {
  chainCode: string
  provider: string
  rpcUrl?: string
  apiKeyMasked?: string
  apiKeyConfigured: boolean
  apiKeyCount?: number
  apiKeys?: ChainNodeApiKeyItem[]
  apiUrl?: string
  rpcUser?: string
  rpcPasswordConfigured: boolean
  effectiveRpcUrlMasked?: string
  requiredConfirms?: number
  isEnabled: number
  scanReady: boolean
  remark?: string
  updatedAt?: string
}

export interface ChainNodeConfigUpdateReq {
  provider: string
  rpcUrl?: string
  apiKey?: string
  apiUrl?: string
  rpcUser?: string
  rpcPassword?: string
  requiredConfirms?: number
  isEnabled?: number
  remark?: string
}

export function getChainNodeList(): Promise<ChainNodeConfig[]> {
  return adminHttp.get('/chain-nodes')
}

export function getChainNodeDetail(chainCode: string): Promise<ChainNodeConfig> {
  return adminHttp.get(`/chain-nodes/${chainCode}`)
}

export function updateChainNode(
  chainCode: string,
  data: ChainNodeConfigUpdateReq,
): Promise<ChainNodeConfig> {
  return adminHttp.put(`/chain-nodes/${chainCode}`, data)
}

export function getChainNodeApiKeys(chainCode: string): Promise<ChainNodeApiKeyItem[]> {
  return adminHttp.get(`/chain-nodes/${chainCode}/api-keys`)
}

export function addChainNodeApiKey(
  chainCode: string,
  data: { apiKey: string; label?: string },
): Promise<ChainNodeApiKeyItem> {
  return adminHttp.post(`/chain-nodes/${chainCode}/api-keys`, data)
}

export function deleteChainNodeApiKey(chainCode: string, keyId: number): Promise<void> {
  return adminHttp.delete(`/chain-nodes/${chainCode}/api-keys/${keyId}`)
}
