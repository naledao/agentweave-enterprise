import { render, screen } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { authApi } from '@/features/auth/api/authApi'
import LoginView from '@/features/auth/views/LoginView.vue'

vi.mock('@/features/auth/api/authApi', () => ({
  authApi: {
    login: vi.fn(),
    getCurrentUser: vi.fn(),
  },
}))

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('validates required fields', async () => {
    const router = createTestRouter()
    render(LoginView, {
      global: {
        plugins: [createPinia(), router],
      },
    })

    await userEvent.click(screen.getByRole('button', { name: '登录' }))

    expect(await screen.findByText('请输入用户名')).toBeInTheDocument()
    expect(await screen.findByText('请输入密码')).toBeInTheDocument()
  })

  it('shows login error', async () => {
    vi.mocked(authApi.login).mockRejectedValue({
      code: 'AUTH_001',
      message: 'Username or password is incorrect',
      traceId: 'trace-1',
      path: '/api/v1/auth/login',
      timestamp: new Date().toISOString(),
    })
    const router = createTestRouter()
    render(LoginView, {
      global: {
        plugins: [createPinia(), router],
      },
    })

    await userEvent.type(screen.getByPlaceholderText('请输入用户名'), 'admin')
    await userEvent.type(screen.getByPlaceholderText('请输入密码'), 'password123')
    await userEvent.click(screen.getByRole('button', { name: '登录' }))

    expect(await screen.findByText(/Username or password is incorrect/)).toBeInTheDocument()
    expect(await screen.findByText(/traceId: trace-1/)).toBeInTheDocument()
  })
})

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/app/chat', component: { template: '<div />' } },
    ],
  })
}
