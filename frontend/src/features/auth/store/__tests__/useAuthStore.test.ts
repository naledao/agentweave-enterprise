import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { authApi } from '@/features/auth/api/authApi'
import { clearAuthState } from '@/features/auth/store/authSession'
import { useAuthStore } from '@/features/auth/store/useAuthStore'
import type { CurrentUser, LoginResponse } from '@/features/auth/types'

vi.mock('@/features/auth/api/authApi', () => ({
  authApi: {
    login: vi.fn(),
    getCurrentUser: vi.fn(),
    logout: vi.fn(),
  },
}))

const currentUser: CurrentUser = {
  id: 'user-1',
  username: 'admin',
  displayName: '管理员',
  email: null,
  status: 'ACTIVE',
  roles: ['ADMIN'],
  permissions: ['auth:user:read'],
  lastLoginAt: null,
}

describe('useAuthStore', () => {
  beforeEach(() => {
    localStorage.clear()
    clearAuthState()
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('saves token and current user after login', async () => {
    const response: LoginResponse = {
      accessToken: 'token-1',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: currentUser,
    }
    vi.mocked(authApi.login).mockResolvedValue(response)

    const store = useAuthStore()
    await store.login({ username: 'admin', password: 'password123' })

    expect(store.token).toBe('token-1')
    expect(store.currentUser?.username).toBe('admin')
    expect(localStorage.getItem('agentweave.access_token')).toBe('token-1')
  })

  it('clears state after logout', async () => {
    vi.mocked(authApi.logout).mockResolvedValue()
    const store = useAuthStore()
    store.token = 'token-1'
    store.currentUser = currentUser

    await store.logout()

    expect(store.token).toBeNull()
    expect(store.currentUser).toBeNull()
    expect(localStorage.getItem('agentweave.access_token')).toBeNull()
  })

  it('checks permissions and admin role', () => {
    const store = useAuthStore()
    store.currentUser = currentUser

    expect(store.hasPermission('auth:user:write')).toBe(true)
    expect(store.hasAnyPermission(['missing', 'auth:user:read'])).toBe(true)
    expect(store.hasRole('ADMIN')).toBe(true)
  })
})
