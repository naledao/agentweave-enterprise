import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import AssistantGenerationPanel from '@/features/chat/components/AssistantGenerationPanel.vue'
import type { ChatMessage } from '@/features/chat/types'

const pendingAssistantMessage: ChatMessage = {
  id: 'assistant-pending-1',
  conversationId: 'conversation-1',
  role: 'ASSISTANT',
  content: '',
  status: 'PENDING',
  errorCode: null,
  errorMessage: null,
  metadata: '{}',
  traceId: 'trace-1',
  citations: [],
  graphPaths: [],
  toolCalls: [],
  createdAt: '2026-05-12T00:11:45.000Z',
}

describe('AssistantGenerationPanel', () => {
  it('shows assistant placeholder status outside the message stream', () => {
    render(AssistantGenerationPanel, {
      props: {
        messages: [pendingAssistantMessage],
      },
    })

    expect(screen.getByText('生成状态')).toBeInTheDocument()
    expect(screen.getByText('等待生成')).toBeInTheDocument()
    expect(screen.getByText('助手消息已创建，等待模型开始输出。')).toBeInTheDocument()
  })

  it('shows an empty state when no placeholder exists', () => {
    render(AssistantGenerationPanel, {
      props: {
        messages: [],
      },
    })

    expect(screen.getByText('暂无生成任务')).toBeInTheDocument()
  })
})
