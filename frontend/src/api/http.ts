import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiEnvelope } from '@/types'

export const TOKEN_STORAGE_KEY = 'animeverse_token'
export const USER_STORAGE_KEY = 'animeverse_user'

interface AppRequestConfig extends AxiosRequestConfig {
  suppressErrorMessage?: boolean
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const message =
      error.response?.data?.message ||
      (status === 401 ? '登录状态已失效，请重新登录' : '请求失败，请检查网络或服务状态')

    if (status === 401) {
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      localStorage.removeItem(USER_STORAGE_KEY)
      window.dispatchEvent(new CustomEvent('auth-expired'))

      if (!window.location.pathname.endsWith('/login')) {
        const redirect = encodeURIComponent(
          `${window.location.pathname}${window.location.search}${window.location.hash}`
        )
        window.setTimeout(() => {
          window.location.assign(`/login?redirect=${redirect}`)
        }, 100)
      }
    }

    if (!(error.config as AppRequestConfig | undefined)?.suppressErrorMessage) {
      ElMessage.error(message)
    }

    return Promise.reject(new Error(message))
  }
)

export async function request<T>(config: AppRequestConfig): Promise<T> {
  const response = await http.request<ApiEnvelope<T>>(config)
  const envelope = response.data

  if (
    typeof envelope?.code === 'number' &&
    (envelope.code < 200 || envelope.code >= 300)
  ) {
    throw new Error(envelope.message || '请求失败')
  }

  return envelope?.data as T
}

export default http
