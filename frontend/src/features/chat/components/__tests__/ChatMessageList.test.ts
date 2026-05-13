import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import ChatMessageList from '@/features/chat/components/ChatMessageList.vue'
import type { ChatMessage, ChatStreamState } from '@/features/chat/types'

const baseMessage: ChatMessage = {
  id: 'assistant-1',
  conversationId: 'conversation-1',
  role: 'ASSISTANT',
  content: '正式助手回复',
  status: 'SUCCEEDED',
  errorCode: null,
  errorMessage: null,
  metadata: '{}',
  traceId: null,
  citations: [],
  toolCalls: [],
  createdAt: '2026-05-11T15:47:05.485Z',
}

const completedStream: ChatStreamState = {
  status: 'completed',
  assistantMessageId: 'assistant-1',
  content: '正式助手回复',
  seenEventIds: ['done-1'],
  citations: [],
  toolInvocations: [],
  workflowSteps: [],
  error: null,
}

function renderMessageList(messages: ChatMessage[], stream: ChatStreamState = completedStream) {
  return render(ChatMessageList, {
    props: {
      messages,
      stream,
      page: 0,
      size: 20,
      total: messages.length,
    },
  })
}

describe('ChatMessageList', () => {
  it('hides completed stream when the saved assistant message is loaded', () => {
    renderMessageList([baseMessage])

    expect(screen.getAllByText('正式助手回复')).toHaveLength(1)
  })

  it('keeps showing stream content before the saved assistant message arrives', () => {
    renderMessageList([], completedStream)

    expect(screen.getByText('正式助手回复')).toBeInTheDocument()
  })
})
