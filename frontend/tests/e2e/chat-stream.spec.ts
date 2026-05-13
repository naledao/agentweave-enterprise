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

test.describe('流式对话', () => {
  test('发送消息后展示 SSE 增量、工具调用、引用和完成状态', async ({ page }) => {
    await mockLogin(page, adminUser)
    await mockChatApi(page)

    await page.goto('/login')
    await signIn(page, 'admin')

    await expect(page).toHaveURL(/\/app\/chat/)
    await page.getByPlaceholder('输入问题，Agent 会结合知识库、工具调用和执行状态回答').fill('排查订单接口 500')
    await page.getByRole('button', { name: '发送' }).click()

    await expect(page.getByText('排查订单接口 500')).toBeVisible()
    await expect(page.getByText('Planner')).toBeVisible()
    await expect(page.getByText('日志检索')).toBeVisible()
    await expect(page.getByText('已查询近 15 分钟错误日志')).toBeVisible()
    await expect(page.getByText('排障手册')).toBeVisible()
    await expect(page.getByText('订单接口 500 优先检查依赖服务状态。')).toBeVisible()
    await expect(page.getByText('接口近期有 500 错误，建议先检查订单服务依赖。').first()).toBeVisible()
    await expect(page.getByText('完成').first()).toBeVisible()
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

  await page.route('**/api/v1/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(user),
    })
  })
}

async function mockChatApi(page: Page): Promise<void> {
  const now = new Date('2026-05-11T08:00:00.000Z').toISOString()
  const conversationId = '11111111-1111-1111-1111-111111111111'
  const userMessageId = '22222222-2222-2222-2222-222222222222'
  const assistantMessageId = '33333333-3333-3333-3333-333333333333'
  let userMessageContent = ''
  let assistantPersisted = false

  await page.route('**/api/v1/conversations**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const method = request.method()
    const path = url.pathname

    if (method === 'GET' && path === '/api/v1/conversations') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          items: userMessageContent
            ? [
                {
                  id: conversationId,
                  title: '订单接口排障',
                  status: 'ACTIVE',
                  messageCount: 2,
                  lastMessagePreview: assistantPersisted
                    ? '接口近期有 500 错误，建议先检查订单服务依赖。'
                    : userMessageContent,
                  lastMessageAt: now,
                  createdAt: now,
                  updatedAt: now,
                },
              ]
            : [],
          total: userMessageContent ? 1 : 0,
          totalPages: userMessageContent ? 1 : 0,
          page: 0,
          size: 20,
        }),
      })
      return
    }

    if (method === 'POST' && path === '/api/v1/conversations') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(conversationDetail({
          conversationId,
          title: '订单接口排障',
          now,
          messages: [],
        })),
      })
      return
    }

    if (method === 'GET' && path === `/api/v1/conversations/${conversationId}`) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(conversationDetail({
          conversationId,
          title: '订单接口排障',
          now,
          messages: conversationMessages({
            conversationId,
            userMessageId,
            assistantMessageId,
            userMessageContent,
            assistantPersisted,
            now,
          }),
        })),
      })
      return
    }

    if (method === 'POST' && path === `/api/v1/conversations/${conversationId}/messages`) {
      const body = request.postDataJSON() as { content?: string }
      userMessageContent = body.content ?? ''
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          conversationId,
          userMessageId,
          assistantMessageId,
          traceId: 'trace-e2e',
        }),
      })
      return
    }

    if (method === 'GET' && path === `/api/v1/conversations/${conversationId}/stream`) {
      assistantPersisted = true
      await route.fulfill({
        status: 200,
        contentType: 'text/event-stream',
        headers: {
          'Cache-Control': 'no-cache',
        },
        body: [
          sse('workflow_step', {
            eventId: 'evt-workflow-1',
            conversationId,
            messageId: assistantMessageId,
            workflowRunId: 'planner',
            stepName: 'Planner',
            status: 'SUCCEEDED',
            traceId: 'trace-e2e',
            timestamp: now,
          }),
          sse('tool_call_started', {
            eventId: 'evt-tool-1',
            conversationId,
            messageId: assistantMessageId,
            toolCallId: 'tool-log-search',
            toolName: '日志检索',
            inputSummary: '查询近 15 分钟错误日志',
            traceId: 'trace-e2e',
            timestamp: now,
          }),
          sse('tool_call_finished', {
            eventId: 'evt-tool-2',
            conversationId,
            messageId: assistantMessageId,
            toolCallId: 'tool-log-search',
            status: 'SUCCEEDED',
            resultSummary: '已查询近 15 分钟错误日志',
            latencyMs: 18,
            traceId: 'trace-e2e',
            timestamp: now,
          }),
          sse('citation', {
            eventId: 'evt-citation-1',
            conversationId,
            messageId: assistantMessageId,
            documentId: 'runbook-order-api',
            chunkId: 'citation-runbook',
            title: '排障手册',
            snippet: '订单接口 500 优先检查依赖服务状态。',
            score: 0.912,
            traceId: 'trace-e2e',
            timestamp: now,
          }),
          sse('message_delta', {
            eventId: 'evt-delta-1',
            conversationId,
            messageId: assistantMessageId,
            delta: '接口近期有 500 错误，',
            traceId: 'trace-e2e',
            timestamp: now,
          }),
          sse('message_delta', {
            eventId: 'evt-delta-2',
            conversationId,
            messageId: assistantMessageId,
            delta: '建议先检查订单服务依赖。',
            traceId: 'trace-e2e',
            timestamp: now,
          }),
          sse('done', {
            eventId: 'evt-done',
            conversationId,
            messageId: assistantMessageId,
            status: 'SUCCEEDED',
            traceId: 'trace-e2e',
            timestamp: now,
          }),
        ].join(''),
      })
      return
    }

    await route.fulfill({
      status: 404,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'E2E_NOT_FOUND',
        message: `${method} ${path} is not mocked`,
        path,
        traceId: 'trace-e2e',
        timestamp: now,
      }),
    })
  })
}

function conversationDetail(input: {
  conversationId: string
  title: string
  now: string
  messages: unknown[]
}) {
  return {
    id: input.conversationId,
    title: input.title,
    status: 'ACTIVE',
    messageCount: input.messages.length,
    lastMessagePreview: input.messages.at(-1) ? '接口近期有 500 错误，建议先检查订单服务依赖。' : null,
    lastMessageAt: input.messages.at(-1) ? input.now : null,
    createdAt: input.now,
    updatedAt: input.now,
    messages: input.messages,
    messagePage: 0,
    messageSize: 20,
    messageTotal: input.messages.length,
    messageTotalPages: input.messages.length ? 1 : 0,
    traceId: 'trace-e2e',
  }
}

function conversationMessages(input: {
  conversationId: string
  userMessageId: string
  assistantMessageId: string
  userMessageContent: string
  assistantPersisted: boolean
  now: string
}) {
  const messages = input.userMessageContent
    ? [
        {
          id: input.userMessageId,
          conversationId: input.conversationId,
          role: 'USER',
          content: input.userMessageContent,
          status: 'SUCCEEDED',
          citations: [],
          toolCalls: [],
          createdAt: input.now,
        },
      ]
    : []

  if (input.userMessageContent) {
    messages.push({
      id: input.assistantMessageId,
      conversationId: input.conversationId,
      role: 'ASSISTANT',
      content: input.assistantPersisted ? '接口近期有 500 错误，建议先检查订单服务依赖。' : '',
      status: input.assistantPersisted ? 'SUCCEEDED' : 'PENDING',
      citations: [],
      toolCalls: [],
      createdAt: input.now,
    })
  }

  return messages
}

function sse(event: string, data: unknown): string {
  return `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`
}
