import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import AuditLogTable from '@/features/observability/components/AuditLogTable.vue'
import ModelCallTable from '@/features/observability/components/ModelCallTable.vue'
import type { AuditLog, ModelCall } from '@/features/observability/types'

describe('observability tables', () => {
  it('renders model call rows with trace ids and summaries', async () => {
    render(ModelCallTable, {
      props: {
        calls: [modelCall()],
      },
    })

    expect(await screen.findByText('失败')).toBeInTheDocument()
    expect(screen.getByText('qwen-max')).toBeInTheDocument()
    expect(screen.getByText('RAG 回答')).toBeInTheDocument()
    expect(screen.getByText('model failed')).toBeInTheDocument()
    expect(screen.getByText('trace-model-001')).toBeInTheDocument()
  })

  it('renders audit log rows with sanitized summaries', async () => {
    render(AuditLogTable, {
      props: {
        logs: [auditLog()],
      },
    })

    expect(await screen.findByText('拒绝')).toBeInTheDocument()
    expect(screen.getByText('工具拒绝')).toBeInTheDocument()
    expect(screen.getByText('tool')).toBeInTheDocument()
    expect(screen.getByText('input summary')).toBeInTheDocument()
    expect(screen.getByText('trace-audit-001')).toBeInTheDocument()
  })
})

function modelCall(): ModelCall {
  return {
    id: 'model-call-1',
    traceId: 'trace-model-001',
    conversationId: 'conversation-1',
    messageId: 'message-1',
    workflowRunId: null,
    workflowStepId: null,
    provider: 'dashscope',
    modelName: 'qwen-max',
    scenario: 'RAG_ANSWER',
    promptSummary: 'prompt summary',
    responseSummary: null,
    inputTokens: 10,
    outputTokens: 20,
    totalTokens: 30,
    durationMs: 1200,
    status: 'FAILED',
    errorCode: 'MODEL_ERROR',
    errorMessage: 'model failed',
    createdAt: '2026-05-14T00:00:00Z',
  }
}

function auditLog(): AuditLog {
  return {
    id: 'audit-log-1',
    eventType: 'TOOL_PERMISSION_DENIED',
    resourceType: 'tool',
    resourceId: 'tool-1',
    userId: 'user-1',
    username: 'admin',
    result: 'DENIED',
    durationMs: 8,
    requestSummary: 'input summary',
    responseSummary: null,
    errorMessage: null,
    traceId: 'trace-audit-001',
    createdAt: '2026-05-14T00:00:00Z',
  }
}
