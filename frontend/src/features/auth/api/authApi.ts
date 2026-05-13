import { httpClient } from '@/shared/api/httpClient'
import type { CurrentUser, LoginRequest, LoginResponse } from '../types'

export const authApi = {
  async login(request: LoginRequest): Promise<LoginResponse> {
    const { data } = await httpClient.post<LoginResponse>('/auth/login', request)
    return data
  },

  async getCurrentUser(): Promise<CurrentUser> {
    const { data } = await httpClient.get<CurrentUser>('/auth/me')
    return data
  },

  async logout(): Promise<void> {
    await httpClient.post('/auth/logout')
  },
}
