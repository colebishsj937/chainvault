import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useMerchantStore = defineStore('merchant', () => {
  const tier = ref(1)

  function setTier(t: number) {
    tier.value = t
  }

  return { tier, setTier }
})
