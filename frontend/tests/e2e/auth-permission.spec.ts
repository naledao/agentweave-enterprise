import { expect, type Page, test } from '@playwright/test'

interface MockUser {
  id: string
  username: string
  displayName: string
  email: string | null
  status: 'ACTIVE'
  roles: string[]
  permissions: string[]
  lastLoginAt: string | null
}

const adminUser: MockUser = {
  id: '00000000-0000-0000-0000-000000000001',
  username: 'admin',
  displayName: '平台管理员',
  email: 'admin@example.com',
  status: 'ACTIVE',
  roles: ['ADMIN'],
  permissions: ['auth:user:read', 'auth:user:write', 'auth:role:read', 'auth:role:write'],
  lastLoginAt: null,
}

const limitedUser: MockUser = {
  id: '00000000-0000-0000-0000-000000000002',
  username: 'viewer',
  displayName: '普通用户',
  email: 'viewer@example.com',
  status: 'ACTIVE',
  roles: ['VIEWER'],
  permissions: [],
  lastLoginAt: null,
}

test.describe('认证与权限', () => {
  test('未登录访问 /app/chat 跳转 /login', async ({ page }) => {
    await page.goto('/app/chat')

    await expect(page).toHaveURL(/\/login\?redirect=\/app\/chat/)
    await expect(page.getByRole('heading', { name: 'AgentWeave Enterprise' })).toBeVisible()
  })

  test('登录成功后跳转 /app/chat', async ({ page }) => {
    await mockLogin(page, adminUser)

    await page.goto('/login')
    await signIn(page, 'admin')

    await expect(page).toHaveURL(/\/app\/chat/)
    await expect(page.getByText('企业知识与任务编排')).toBeVisible()
    await expect(page.getByRole('menuitem', { name: '会话' })).toBeVisible()
    await expect(page.getByRole('button', { name: /平台管理员/ })).toBeVisible()
  })

  test('刷新后仍保持登录态', async ({ page }) => {
    await mockLogin(page, adminUser)
    await mockCurrentUser(page, adminUser)

    await page.goto('/login')
    await signIn(page, 'admin')
    await page.reload()

    await expect(page).toHaveURL(/\/app\/chat/)
    await expect(page.getByText('企业知识与任务编排')).toBeVisible()
    await expect(page.getByRole('menuitem', { name: '会话' })).toBeVisible()
    await expect(page.getByRole('button', { name: /平台管理员/ })).toBeVisible()
  })

  test('401 后清理登录态并跳转登录页', async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('agentweave.access_token', 'expired-token')
    })
    await page.route('**/api/v1/auth/me', async (route) => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'AUTH_401',
          message: 'Unauthorized',
          path: '/api/v1/auth/me',
          traceId: 'trace-expired',
          timestamp: new Date().toISOString(),
        }),
      })
    })

    await page.goto('/app/chat')

    await expect(page).toHaveURL(/\/login/)
    await expect(page.getByRole('heading', { name: 'AgentWeave Enterprise' })).toBeVisible()
    await expect(page.evaluate(() => localStorage.getItem('agentweave.access_token'))).resolves.toBeNull()
  })

  test('无权限用户看不到用户管理菜单', async ({ page }) => {
    await mockLogin(page, limitedUser)

    await page.goto('/login')
    await signIn(page, 'viewer')

    await expect(page).toHaveURL(/\/app\/chat/)
    await expect(page.getByRole('menuitem', { name: '对话' })).toBeVisible()
    await expect(page.getByRole('menuitem', { name: '会话' })).toBeVisible()
    await expect(page.getByRole('menuitem', { name: '用户管理' })).toHaveCount(0)
    await expect(page.getByRole('menuitem', { name: '角色管理' })).toHaveCount(0)
  })

  test('无权限用户直接访问用户管理跳转 403', async ({ page }) => {
    await mockLogin(page, limitedUser)

    await page.goto('/login?redirect=/app/settings/users')
    await signIn(page, 'viewer')

    await expect(page).toHaveURL(/\/app\/403/)
    await expect(page.getByText('无权限访问')).toBeVisible()
  })
})

async function signIn(page: Page, username: string): Promise<void> {
  await page.getByPlaceholder('请输入用户名').fill(username)
  await page.getByPlaceholder('请输入密码').fill('password123')
  await page.getByRole('button', { name: '登录' }).click()
}

async function mockLogin(page: Page, user: MockUser): Promise<void> {
  await page.route('**/api/v1/auth/login', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        accessToken: `token-${user.username}`,
        tokenType: 'Bearer',
        expiresIn: 3600,
        user,
      }),
    })
  })
}

async function mockCurrentUser(page: Page, user: MockUser): Promise<void> {
  await page.route('**/api/v1/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(user),
    })
  })
}
