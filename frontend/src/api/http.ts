import axios, { type InternalAxiosRequestConfig, type AxiosRequestConfig } from 'axios'
import { sign } from '@/utils/sign'
import { ElMessage } from 'element-plus'

const instance = axios.create({ baseURL: '/api/v1', timeout: 15000 })

instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const apiKey = sessionStorage.getItem('cv_api_key') || ''
  const secretKey = '' // secretKey 不持久化，仅从 Pinia 内存读取
  const body = config.data ? JSON.stringify(config.data) : ''
  const signed = sign(body, secretKey)

  config.headers['X-Api-Key'] = apiKey
  config.headers['X-Timestamp'] = signed.timestamp
  config.headers['X-Nonce'] = signed.nonce
  config.headers['X-Sign'] = signed.sign
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
    ElMessage.error(err.response?.data?.message || '网络错误')
    return Promise.reject(err)
  },
)

const http = {
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

export default http
