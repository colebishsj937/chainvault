import axios, { type InternalAxiosRequestConfig, type AxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const instance = axios.create({
  baseURL: '/admin/api/v1',
  timeout: 15000,
})

instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  config.headers['Content-Type'] = 'application/json'
  return config
})

instance.interceptors.response.use(
  (res) => {
    const data = res.data
    if (data.code !== 0) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data.data
  },
  (err) => {
    if (err.response?.status === 401) {
      useAuthStore().logout()
      window.location.href = '/login'
    }
    const msg = err.response?.data?.message || err.response?.data?.msg || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(err)
  },
)

const adminHttp = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.get(url, config) as Promise<T>
  },
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return instance.post(url, data, config) as Promise<T>
  },
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return instance.put(url, data, config) as Promise<T>
  },
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.delete(url, config) as Promise<T>
  },
}

export default adminHttp
