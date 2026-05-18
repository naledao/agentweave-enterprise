import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { authApi } from '@/features/auth/api/authApi'
import { clearAuthState, setAuthToken } from '@/features/auth/store/authSession'
import { useAuthStore } from '@/features/auth/store/useAuthStore'
import { router } from '@/app/router'
import type { CurrentUser } from '@/features/auth/types'

vi.mock('@/features/auth/api/authApi', () => ({
  authApi: {
    getCurrentUser: vi.fn(),
    login: vi.fn(),
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
  permissions: [],
  lastLoginAt: null,
}

describe('auth route guard', () => {
  beforeEach(async () => {
    localStorage.clear()
    clearAuthState()
    setActivePinia(createPinia())
    vi.clearAllMocks()
    await router.replace('/login')
  })

  it('redirects anonymous users to login with the original path', async () => {
    await router.push('/app/chat')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('Login')
    expect(router.currentRoute.value.query.redirect).toBe('/app/chat')
  })

  it('redirects authenticated users away from login', async () => {
    const authStore = useAuthStore()
    authStore.token = 'token-1'
    authStore.currentUser = currentUser
    authStore.initialized = true

    await router.push('/login?redirect=/app/tools')
    await router.isReady()

    expect(router.currentRoute.value.fullPath).toBe('/app/tools')
  })

  it('restores current user before entering protected routes', async () => {
    setAuthToken('token-1')
    vi.mocked(authApi.getCurrentUser).mockResolvedValue(currentUser)

    await router.push('/app/conversations')
    await router.isReady()

    expect(authApi.getCurrentUser).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value.fullPath).toBe('/app/conversations')
  })
})
