import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import ToolInvocationDetailDrawer from '@/features/tools/components/ToolInvocationDetailDrawer.vue'
import type { ToolInvocationDetail } from '@/features/tools/types'

describe('ToolInvocationDetailDrawer', () => {
  it('renders invocation summaries and trace id', async () => {
    render(ToolInvocationDetailDrawer, {
      props: {
        modelValue: true,
        loading: false,
        error: { message: '', traceId: null },
        invocation: {
          id: crypto.randomUUID(),
          toolCode: 'endpoint.status',
          toolName: '接口状态查询',
          toolType: 'ENDPOINT_STATUS',
          riskLevel: 'LOW',
          userId: crypto.randomUUID(),
          username: 'ops-user',
          conversationId: 'conversation-1',
          messageId: 'message-1',
          workflowRunId: 'workflow-run-1',
          workflowStepId: 'workflow-step-1',
          inputSummary: '{"service":"payment"}',
          resultSummary: '{"status":"UP"}',
          status: 'success',
          durationMs: 76,
          errorMessage: null,
          traceId: 'trace-detail',
          createdAt: '2026-05-14T00:00:00Z',
          finishedAt: '2026-05-14T00:00:01Z',
        } satisfies ToolInvocationDetail,
      },
    })

    expect(await screen.findByText('endpoint.status')).toBeInTheDocument()
    expect(screen.getByText('接口状态查询')).toBeInTheDocument()
    expect(screen.getByText('接口状态')).toBeInTheDocument()
    expect(screen.getByText('低风险')).toBeInTheDocument()
    expect(screen.getByText('ops-user')).toBeInTheDocument()
    expect(screen.getByText('成功')).toBeInTheDocument()
    expect(screen.getByText('{"service":"payment"}')).toBeInTheDocument()
    expect(screen.getByText('{"status":"UP"}')).toBeInTheDocument()
    expect(screen.getByText('workflow-run-1')).toBeInTheDocument()
    expect(screen.getByText('workflow-step-1')).toBeInTheDocument()
    expect(screen.getByText('trace-detail')).toBeInTheDocument()
  })

  it('renders load errors with trace id', async () => {
    render(ToolInvocationDetailDrawer, {
      props: {
        modelValue: true,
        loading: false,
        invocation: null,
        error: { message: '工具调用详情加载失败', traceId: 'trace-error' },
      },
    })

    expect(await screen.findByText('工具调用详情加载失败')).toBeInTheDocument()
    expect(screen.getByText('trace-error')).toBeInTheDocument()
  })
})
