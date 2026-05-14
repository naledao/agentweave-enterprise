import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import ToolInvocationTable from '@/features/tools/components/ToolInvocationTable.vue'
import type { ToolInvocation } from '@/features/tools/types'

describe('ToolInvocationTable', () => {
  it('renders invocation rows with status and trace id', async () => {
    render(ToolInvocationTable, {
      props: {
        invocations: [
          toolInvocation({
            toolCode: 'ticket.query',
            username: 'alice',
            status: 'success',
            durationMs: 128,
            traceId: 'trace-success',
          }),
          toolInvocation({
            toolCode: 'log.search',
            username: 'bob',
            status: 'failed',
            errorMessage: '日志服务超时',
            traceId: 'trace-failed',
          }),
        ],
      },
    })

    expect(await screen.findByText('ticket.query')).toBeInTheDocument()
    expect(screen.getByText('alice')).toBeInTheDocument()
    expect(screen.getByText('成功')).toBeInTheDocument()
    expect(screen.getByText('128 ms')).toBeInTheDocument()
    expect(screen.getByText('log.search')).toBeInTheDocument()
    expect(screen.getByText('失败')).toBeInTheDocument()
    expect(screen.getByText('日志服务超时')).toBeInTheDocument()
    expect(screen.getByText('trace-failed')).toBeInTheDocument()
  })
})

function toolInvocation(overrides: Partial<ToolInvocation>): ToolInvocation {
  return {
    id: crypto.randomUUID(),
    toolCode: 'tool.code',
    userId: crypto.randomUUID(),
    username: 'user',
    conversationId: null,
    messageId: null,
    inputSummary: '{"keyword":"demo"}',
    resultSummary: '{"count":1}',
    status: 'running',
    durationMs: null,
    errorMessage: null,
    traceId: null,
    createdAt: '2026-05-14T00:00:00Z',
    finishedAt: null,
    ...overrides,
  }
}
