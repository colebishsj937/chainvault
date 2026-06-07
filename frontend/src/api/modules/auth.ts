import adminHttp from '@/api/adminHttp'

export interface AdminUser {
  id: number
  username: string
  displayName: string
  role: string
}

export interface LoginResult {
  token: string
  tokenType: string
  expiresIn: number
  user: AdminUser
}

export function login(username: string, password: string) {
  return adminHttp.post<LoginResult>('/auth/login', { username, password })
}

export function logout() {
  return adminHttp.post<void>('/auth/logout')
}

export function getCurrentUser() {
  return adminHttp.get<AdminUser>('/auth/me')
}
