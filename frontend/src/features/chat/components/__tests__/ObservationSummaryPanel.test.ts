import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import ObservationSummaryPanel from '@/features/chat/components/ObservationSummaryPanel.vue'
import { createInitialStreamState } from '@/features/chat/composables/streamReducer'
import type { ChatMessage } from '@/features/chat/types'

describe('ObservationSummaryPanel', () => {
  it('renders trace id and aggregated call chain summary', () => {
    render(ObservationSummaryPanel, {
      props: {
        message: message(),
        stream: createInitialStreamState(),
        conversationTraceId: 'trace-conversation',
        citations: [
          {
            title: 'Order FAQ',
            snippet: 'timeout guidance',
            score: 0.91,
          },
        ],
        graphPaths: [
          {
            depth: 2,
            entities: ['Order Service', 'Payment Service'],
            relationships: ['DEPENDS_ON'],
            sourceChunkIds: ['chunk-1'],
            confidence: 0.8,
          },
        ],
        toolInvocations: [
          {
            toolCallId: 'tool-call-1',
            toolName: '日志查询',
            status: 'SUCCEEDED',
            traceId: 'trace-tool',
          },
        ],
        workflowSteps: [
          {
            workflowRunId: 'run-1',
            stepName: 'Planner',
            status: 'SUCCEEDED',
            traceId: 'trace-step',
          },
        ],
      },
    })

    expect(screen.getByText('调用链摘要')).toBeInTheDocument()
    expect(screen.getAllByText('trace-tool').length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText('Vector RAG')).toBeInTheDocument()
    expect(screen.getByText('GraphRAG')).toBeInTheDocument()
    expect(screen.getByText('工具调用')).toBeInTheDocument()
    expect(screen.getByText('工作流步骤')).toBeInTheDocument()
    expect(screen.getByText('Order FAQ')).toBeInTheDocument()
    expect(screen.getByText('Order Service -> Payment Service')).toBeInTheDocument()
  })
})

function message(): ChatMessage {
  return {
    id: 'message-1',
    conversationId: 'conversation-1',
    role: 'ASSISTANT',
    content: 'answer',
    status: 'SUCCEEDED',
    errorCode: null,
    errorMessage: null,
    metadata: '{}',
    traceId: 'trace-message',
    citations: [],
    graphPaths: [],
    toolCalls: [],
    createdAt: '2026-05-14T00:00:00Z',
  }
}
