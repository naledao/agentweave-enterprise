import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import GraphRagSummaryPanel from '@/features/observability/components/GraphRagSummaryPanel.vue'
import type { GraphRagSummary } from '@/features/observability/types'

describe('GraphRagSummaryPanel', () => {
  it('renders GraphRAG counts, durations and trace ids', () => {
    render(GraphRagSummaryPanel, {
      props: {
        summary: graphRagSummary(),
      },
    })

    expect(screen.getByText('构建日志')).toBeInTheDocument()
    expect(screen.getByText('检索日志')).toBeInTheDocument()
    expect(screen.getByText('31 ms')).toBeInTheDocument()
    expect(screen.getByText('trace-index-001')).toBeInTheDocument()
    expect(screen.getByText('trace-retrieval-001')).toBeInTheDocument()
    expect(screen.getByText('latest INDEXED')).toBeInTheDocument()
    expect(screen.getByText('latest SUCCESS')).toBeInTheDocument()
  })
})

function graphRagSummary(): GraphRagSummary {
  return {
    indexLogCount: 3,
    retrievalLogCount: 5,
    latestIndexLog: {
      id: 'index-log-1',
      documentId: 'document-1',
      traceId: 'trace-index-001',
      entityCount: 12,
      relationshipCount: 7,
      chunkCount: 4,
      chunkEntityCount: 16,
      neo4jEnabled: false,
      durationMs: 31,
      status: 'INDEXED',
      errorMessage: null,
      startedAt: '2026-05-14T00:00:00Z',
      completedAt: '2026-05-14T00:00:01Z',
      createdAt: '2026-05-14T00:00:00Z',
    },
    latestRetrievalLog: {
      id: 'retrieval-log-1',
      traceId: 'trace-retrieval-001',
      conversationId: 'conversation-1',
      messageId: 'message-1',
      workflowRunId: 'workflow-run-1',
      workflowStepId: 'workflow-step-1',
      query: 'why order timeout',
      retrievalMode: 'HYBRID',
      businessDomain: 'order',
      permissionLevel: 'INTERNAL',
      documentId: 'document-1',
      maxDepth: 2,
      maxPathCount: 5,
      resolvedEntities: ['Order Service'],
      matchedPathCount: 4,
      filteredPathCount: 2,
      sourceChunkIds: ['chunk-1', 'chunk-2'],
      confidenceSummary: 'count=2',
      durationMs: 18,
      status: 'SUCCESS',
      errorMessage: null,
      startedAt: '2026-05-14T00:00:00Z',
      completedAt: '2026-05-14T00:00:01Z',
      createdAt: '2026-05-14T00:00:00Z',
    },
  }
}
