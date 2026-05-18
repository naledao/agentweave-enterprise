import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import GraphRagIndexLogTable from '@/features/observability/components/GraphRagIndexLogTable.vue'
import GraphRagRetrievalLogTable from '@/features/observability/components/GraphRagRetrievalLogTable.vue'
import type {
  GraphRagIndexLog,
  GraphRagRetrievalLog,
} from '@/features/observability/types'

describe('GraphRag log tables', () => {
  it('renders index log rows', async () => {
    render(GraphRagIndexLogTable, {
      props: {
        logs: [
          indexLog({
            status: 'FAILED',
            errorMessage: 'Neo4j unavailable',
            traceId: 'trace-index-failed',
          }),
        ],
      },
    })

    expect(await screen.findByText('失败')).toBeInTheDocument()
    expect(screen.getByText('Neo4j unavailable')).toBeInTheDocument()
    expect(screen.getByText('trace-index-failed')).toBeInTheDocument()
    expect(screen.getByText('12 ms')).toBeInTheDocument()
  })

  it('renders retrieval log rows', async () => {
    render(GraphRagRetrievalLogTable, {
      props: {
        logs: [
          retrievalLog({
            status: 'DEGRADED',
            errorMessage: 'graph path timeout',
            traceId: 'trace-retrieval-degraded',
          }),
        ],
      },
    })

    expect(await screen.findByText('降级')).toBeInTheDocument()
    expect(screen.getByText('why order timeout')).toBeInTheDocument()
    expect(screen.getByText('Order Service')).toBeInTheDocument()
    expect(screen.getByText('graph path timeout')).toBeInTheDocument()
    expect(screen.getByText('trace-retrieval-degraded')).toBeInTheDocument()
    expect(screen.getByText('workflow-run-1')).toBeInTheDocument()
    expect(screen.getByText('workflow-step-1')).toBeInTheDocument()
  })
})

function indexLog(overrides: Partial<GraphRagIndexLog>): GraphRagIndexLog {
  return {
    id: 'index-log-1',
    documentId: 'document-1',
    traceId: 'trace-index',
    entityCount: 2,
    relationshipCount: 1,
    chunkCount: 1,
    chunkEntityCount: 2,
    neo4jEnabled: false,
    durationMs: 12,
    status: 'INDEXED',
    errorMessage: null,
    startedAt: '2026-05-14T00:00:00Z',
    completedAt: '2026-05-14T00:00:01Z',
    createdAt: '2026-05-14T00:00:00Z',
    ...overrides,
  }
}

function retrievalLog(overrides: Partial<GraphRagRetrievalLog>): GraphRagRetrievalLog {
  return {
    id: 'retrieval-log-1',
    traceId: 'trace-retrieval',
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
    matchedPathCount: 3,
    filteredPathCount: 2,
    sourceChunkIds: ['chunk-1', 'chunk-2'],
    confidenceSummary: 'count=2',
    durationMs: 8,
    status: 'SUCCESS',
    errorMessage: null,
    startedAt: '2026-05-14T00:00:00Z',
    completedAt: '2026-05-14T00:00:01Z',
    createdAt: '2026-05-14T00:00:00Z',
    ...overrides,
  }
}
