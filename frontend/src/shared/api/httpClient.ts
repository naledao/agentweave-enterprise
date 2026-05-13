import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'

import { clearAuthState, getAuthToken } from '@/features/auth/store/authSession'
import type { ApiError } from '@/shared/types/api'
import { createRequestId } from '@/shared/utils/requestId'

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

httpClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAuthToken()
  config.headers.set('X-Request-Id', createRequestId())

  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }

  return config
})

httpClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    const status = error.response?.status
    const data = error.response?.data

    if (status === 401) {
      clearAuthState()
      window.dispatchEvent(new CustomEvent('agentweave:unauthorized'))
    }

    if (data?.code && data.message) {
      return Promise.reject({ ...data, status })
    }

    return Promise.reject({
      code: status ? `HTTP_${status}` : 'NETWORK_ERROR',
      message: status ? '请求处理失败' : '网络连接异常',
      path: error.config?.url ?? '',
      traceId: error.response?.headers['x-trace-id'] ?? '',
      timestamp: new Date().toISOString(),
      status,
    } satisfies ApiError)
  },
)
