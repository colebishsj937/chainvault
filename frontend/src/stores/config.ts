import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface ChainConfig {
  chainCode: string
  chainName: string
  nativeSymbol: string
}

export interface CoinConfig {
  symbol: string
  chainCode: string
  decimals: number
}

export const useConfigStore = defineStore('config', () => {
  const chains = ref<ChainConfig[]>([])
  const coins = ref<CoinConfig[]>([])

  function setChains(list: ChainConfig[]) {
    chains.value = list
  }

  function setCoins(list: CoinConfig[]) {
    coins.value = list
  }

  return { chains, coins, setChains, setCoins }
})
