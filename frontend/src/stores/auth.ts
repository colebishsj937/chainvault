import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AdminUser } from '@/api/modules/auth'

const TOKEN_KEY = 'cv_admin_token'
const USER_KEY = 'cv_admin_user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem(TOKEN_KEY) || '')
  const user = ref<AdminUser | null>(loadUser())

  const isLoggedIn = computed(() => !!token.value)

  function loadUser(): AdminUser | null {
    const raw = sessionStorage.getItem(USER_KEY)
    if (!raw) {
      return null
    }
    try {
      return JSON.parse(raw) as AdminUser
    } catch {
      return null
    }
  }

  function setSession(accessToken: string, adminUser: AdminUser) {
    token.value = accessToken
    user.value = adminUser
    sessionStorage.setItem(TOKEN_KEY, accessToken)
    sessionStorage.setItem(USER_KEY, JSON.stringify(adminUser))
  }

  function logout() {
    token.value = ''
    user.value = null
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(USER_KEY)
  }

  return { token, user, isLoggedIn, setSession, logout }
})
