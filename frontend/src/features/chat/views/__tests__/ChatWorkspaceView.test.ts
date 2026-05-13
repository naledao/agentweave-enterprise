import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { render, screen, waitFor } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { nextTick } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest'

import { openChatStream } from '@/features/chat/api/chatStreamClient'
import { conversationsApi } from '@/features/chat/api/conversationsApi'
import type {
  CancelMessageResponse,
  ChatStreamEvent,
  ConversationCreateResponse,
  ConversationDetail,
  ConversationSummary,
  SendMessageResponse,
} from '@/features/chat/types'
import ChatWorkspaceView from '@/features/chat/views/ChatWorkspaceView.vue'
import type { ItemPageResponse } from '@/shared/types/api'

vi.mock('@/features/chat/api/conversationsApi', () => ({
  conversationsApi: {
    createConversation: vi.fn(),
    queryConversations: vi.fn(),
    getConversation: vi.fn(),
    sendMessage: vi.fn(),
    cancelMessage: vi.fn(),
  },
}))

vi.mock('@/features/chat/api/chatStreamClient', () => ({
  openChatStream: vi.fn(),
}))

const sentMessageResponse: SendMessageResponse = {
  conversationId: 'conversation-1',
  userMessageId: 'user-1',
  assistantMessageId: 'assistant-1',
  traceId: 'trace-send',
}

const conversationsPage: ItemPageResponse<ConversationSummary> = {
  items: [
    conversationSummary('conversation-1', 'Active conversation'),
    conversationSummary('conversation-2', 'Other conversation'),
  ],
  page: 0,
  size: 20,
  total: 2,
  totalPages: 1,
}

let streamHandlers: Parameters<typeof openChatStream>[1] | null
let closeStream: Mock<() => void>

describe('ChatWorkspaceView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    streamHandlers = null
    closeStream = vi.fn()

    vi.mocked(conversationsApi.queryConversations).mockResolvedValue(conversationsPage)
    vi.mocked(conversationsApi.getConversation).mockImplementation(async (conversationId) => {
      return conversationDetail(conversationId, conversationId === 'conversation-2' ? 'Other conversation' : 'Active conversation')
    })
    vi.mocked(conversationsApi.createConversation).mockResolvedValue(newConversation())
    vi.mocked(conversationsApi.sendMessage).mockResolvedValue(sentMessageResponse)
    vi.mocked(conversationsApi.cancelMessage).mockImplementation(async (conversationId, messageId) => {
      return cancelMessageResponse(conversationId, messageId)
    })
    vi.mocked(openChatStream).mockImplementation((_conversationId, handlers) => {
      streamHandlers = handlers
      return { close: () => closeStream() }
    })
  })

  it('closes the stream and calls cancel API when stop is clicked', async () => {
    const { container } = await renderWorkspace()

    await sendMessage(container)
    await userEvent.click(getStopButton(container))

    await waitFor(() => {
      expect(closeStream).toHaveBeenCalled()
      expect(conversationsApi.cancelMessage).toHaveBeenCalledWith('conversation-1', 'assistant-1')
    })
    expect(container.querySelector('.streaming-message')).toBeInTheDocument()
  })

  it('cancels the active assistant message when switching conversations', async () => {
    const { container, router } = await renderWorkspace()

    await sendMessage(container)
    await userEvent.click(screen.getByRole('button', { name: /Other conversation/ }))

    await waitFor(() => {
      expect(closeStream).toHaveBeenCalled()
      expect(conversationsApi.cancelMessage).toHaveBeenCalledWith('conversation-1', 'assistant-1')
    })
    await waitFor(() => {
      expect(router.currentRoute.value.query.conversationId).toBe('conversation-2')
    })
  })

  it('shows failed status, error message, and trace id for stream timeout events', async () => {
    const { container } = await renderWorkspace()

    await sendMessage(container)
    const timeoutEvent: ChatStreamEvent = {
      type: 'error',
      code: 'CHAT_STREAM_TIMEOUT',
      message: 'SSE stream timed out',
      traceId: 'trace-timeout',
      eventId: 'event-timeout',
    }
    streamHandlers?.onEvent(timeoutEvent)
    await nextTick()

    expect(closeStream).toHaveBeenCalled()
    expect(screen.getByText('SSE stream timed out')).toBeInTheDocument()
    expect(screen.getByText('trace-timeout')).toBeInTheDocument()
    expect(container.querySelector('.streaming-message .el-tag--danger')).toBeInTheDocument()
  })

  it('cancels the active stream when the page unmounts', async () => {
    const rendered = await renderWorkspace()

    await sendMessage(rendered.container)
    rendered.unmount()

    await waitFor(() => {
      expect(closeStream).toHaveBeenCalled()
      expect(conversationsApi.cancelMessage).toHaveBeenCalledWith('conversation-1', 'assistant-1')
    })
  })

  it('searches conversations from the sidebar', async () => {
    await renderWorkspace()

    const searchInput = screen.getByPlaceholderText('搜索标题')
    await userEvent.type(searchInput, '订单')

    await waitFor(() => {
      expect(conversationsApi.queryConversations).toHaveBeenLastCalledWith({
        page: 0,
        size: 20,
        keyword: '订单',
      })
    })
  })
})

async function renderWorkspace() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/app/chat',
        name: 'Chat',
        component: ChatWorkspaceView,
      },
    ],
  })

  await router.push({ name: 'Chat', query: { conversationId: 'conversation-1' } })
  await router.isReady()

  const rendered = render(ChatWorkspaceView, {
    global: {
      plugins: [
        router,
        [VueQueryPlugin, { queryClient }],
      ],
    },
  })

  await waitFor(() => {
    expect(screen.getAllByText('Active conversation').length).toBeGreaterThan(0)
  })

  return {
    ...rendered,
    queryClient,
    router,
  }
}

async function sendMessage(container: Element): Promise<void> {
  const textarea = container.querySelector('textarea')
  expect(textarea).not.toBeNull()

  await userEvent.type(textarea as HTMLTextAreaElement, 'Check service status')
  await userEvent.click(getSendButton(container))

  await waitFor(() => {
    expect(conversationsApi.sendMessage).toHaveBeenCalledWith('conversation-1', {
      content: 'Check service status',
      responseMode: 'STREAM',
    })
    expect(openChatStream).toHaveBeenCalledWith('conversation-1', expect.any(Object))
  })
  expect(streamHandlers).not.toBeNull()
}

function getSendButton(container: Element): HTMLButtonElement {
  const button = container.querySelector<HTMLButtonElement>('.input-actions button[type="submit"]')
  expect(button).not.toBeNull()
  return button as HTMLButtonElement
}

function getStopButton(container: Element): HTMLButtonElement {
  const button = container.querySelector<HTMLButtonElement>('.input-actions button[type="button"]')
  expect(button).not.toBeNull()
  return button as HTMLButtonElement
}

function conversationSummary(id: string, title: string): ConversationSummary {
  return {
    id,
    title,
    status: 'ACTIVE',
    messageCount: 0,
    lastMessagePreview: null,
    lastMessageAt: null,
    createdAt: '2026-05-11T08:00:00Z',
    updatedAt: '2026-05-11T08:00:00Z',
  }
}

function conversationDetail(id: string, title: string): ConversationDetail {
  return {
    ...conversationSummary(id, title),
    messages: [],
    messagePage: 0,
    messageSize: 20,
    messageTotal: 0,
    messageTotalPages: 0,
    traceId: `trace-${id}`,
  }
}

function newConversation(): ConversationCreateResponse {
  return {
    id: 'conversation-new',
    title: 'New conversation',
    status: 'ACTIVE',
    messageCount: 0,
    createdAt: '2026-05-11T09:00:00Z',
    updatedAt: '2026-05-11T09:00:00Z',
    traceId: 'trace-new',
  }
}

function cancelMessageResponse(conversationId: string, messageId: string): CancelMessageResponse {
  return {
    conversationId,
    messageId,
    status: 'CANCELLED',
    traceId: 'trace-cancel',
  }
}
