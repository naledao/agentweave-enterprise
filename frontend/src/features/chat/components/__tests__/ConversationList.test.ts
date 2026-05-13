import { fireEvent, render, screen } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import ConversationList from '@/features/chat/components/ConversationList.vue'
import type { ConversationSummary } from '@/features/chat/types'

const conversation: ConversationSummary = {
  id: 'conversation-1',
  title: '排查订单接口超时',
  status: 'ACTIVE',
  messageCount: 6,
  lastMessagePreview: '接口 P95 延迟升高',
  lastMessageAt: '2026-05-11T08:40:00Z',
  createdAt: '2026-05-11T08:00:00Z',
  updatedAt: '2026-05-11T08:40:00Z',
}

describe('ConversationList', () => {
  it('shows empty state', () => {
    render(ConversationList, {
      props: {
        conversations: [],
        activeConversationId: null,
      },
    })

    expect(screen.getByText('暂无会话')).toBeInTheDocument()
  })

  it('renders conversation summary and emits selection', async () => {
    const { emitted } = render(ConversationList, {
      props: {
        conversations: [conversation],
        activeConversationId: null,
      },
    })

    expect(screen.getByText('排查订单接口超时')).toBeInTheDocument()
    expect(screen.getByText('接口 P95 延迟升高')).toBeInTheDocument()
    expect(screen.getByText(/6 条消息/)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /排查订单接口超时/ }))

    expect(emitted('select')).toEqual([['conversation-1']])
  })

  it('emits keyword changes when searchable', async () => {
    const { emitted } = render(ConversationList, {
      props: {
        conversations: [],
        activeConversationId: null,
        searchable: true,
        keyword: '',
      },
    })

    await fireEvent.update(screen.getByPlaceholderText('搜索标题'), '订单')

    expect(emitted('search')?.at(-1)).toEqual(['订单'])
  })

  it('shows an error banner with trace id', () => {
    render(ConversationList, {
      props: {
        conversations: [],
        activeConversationId: null,
        errorMessage: '会话加载失败',
        errorTraceId: 'trace-list-001',
      },
    })

    expect(screen.getByText('会话加载失败')).toBeInTheDocument()
    expect(screen.getByText('trace-list-001')).toBeInTheDocument()
  })
})
