import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import WorkflowStepDetailDrawer from '@/features/workflows/components/WorkflowStepDetailDrawer.vue'
import type { WorkflowApproval, WorkflowStep } from '@/features/workflows/types'

describe('WorkflowStepDetailDrawer', () => {
  it('renders step artifacts and approval details', async () => {
    render(WorkflowStepDetailDrawer, {
      props: {
        modelValue: true,
        step: workflowStep(),
        approval: workflowApproval(),
      },
    })

    expect(await screen.findByText('2. Vector RAG')).toBeInTheDocument()
    expect(screen.getByText('restart payment service')).toBeInTheDocument()
    expect(screen.getByText('payment-service')).toBeInTheDocument()
    expect(screen.getByText('DEPENDS_ON')).toBeInTheDocument()
    expect(screen.getAllByText('tool:log:search')).toHaveLength(2)
    expect(screen.getByText('已通过')).toBeInTheDocument()
    expect(screen.getByText('read only approved')).toBeInTheDocument()
  })
})

function workflowStep(): WorkflowStep {
  return {
    stepId: 'step-1',
    stepIndex: 1,
    stepType: 'RAG_SEARCH',
    nodeName: 'rag_node',
    status: 'SUCCEEDED',
    inputSummary: 'instruction=search payment docs',
    outputSummary: 'found payment runbook',
    startedAt: '2026-05-15T10:00:00Z',
    finishedAt: '2026-05-15T10:00:01Z',
    durationMs: 1000,
    retryCount: 0,
    retryReason: null,
    lastRetriedAt: null,
    errorCode: null,
    errorMessage: null,
    citations: [
      {
        documentId: 'doc-1',
        documentName: 'Runbook',
        chunkId: 'chunk-1',
        title: 'Runbook',
        source: 'kb',
        snippet: 'restart payment service',
        score: 0.87,
      },
    ],
    graphPaths: [
      {
        pathId: 'path-1',
        depth: 1,
        entities: ['payment-service', 'payment-api'],
        relationships: ['DEPENDS_ON'],
        sourceChunkIds: ['chunk-1'],
        confidence: 0.92,
      },
    ],
    toolCalls: [
      {
        toolCode: 'tool:log:search',
        status: 'success',
        inputSummary: 'keyword=payment',
        resultSummary: '10 rows',
      },
    ],
  }
}

function workflowApproval(): WorkflowApproval {
  return {
    approvalId: 'approval-1',
    runId: 'run-1',
    stepId: 'step-1',
    stepIndex: 1,
    toolCode: 'tool:log:search',
    riskLevel: 'HIGH',
    requestSummary: 'masked request',
    status: 'APPROVED',
    requestedBy: 'user-1',
    approvedBy: 'admin-1',
    decisionReason: 'read only approved',
    createdAt: '2026-05-15T10:00:00Z',
    decidedAt: '2026-05-15T10:01:00Z',
  }
}
