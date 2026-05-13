import { render, screen } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import GraphRagIndexPanel from '@/features/knowledge/components/GraphRagIndexPanel.vue'

describe('GraphRagIndexPanel', () => {
  it('renders summary data and emits rebuild events', async () => {
    const { emitted } = render(GraphRagIndexPanel, {
      props: {
        graphRag: {
          status: 'indexed',
          entityCount: 12,
          relationshipCount: 7,
          chunkCount: 5,
          errorMessage: null,
          traceId: 'trace-graph-001',
          indexedAt: '2026-05-13T08:00:00.000Z',
        },
      },
    })

    expect(screen.getByText('已完成')).toBeInTheDocument()
    expect(screen.getByText('图谱索引可用于 GraphRAG 检索')).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()
    expect(screen.getByText('7')).toBeInTheDocument()
    expect(screen.getByText('5')).toBeInTheDocument()
    expect(screen.getByText('trace-graph-001')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: '重建图谱索引' }))

    expect(emitted().rebuild).toHaveLength(1)
  })

  it('renders failure details', () => {
    render(GraphRagIndexPanel, {
      props: {
        graphRag: {
          status: 'failed',
          entityCount: 2,
          relationshipCount: 1,
          chunkCount: 1,
          errorMessage: 'Neo4j unreachable',
          traceId: 'trace-graph-002',
          indexedAt: null,
        },
      },
    })

    expect(screen.getByText('失败')).toBeInTheDocument()
    expect(screen.getByText('最近一次图谱构建失败')).toBeInTheDocument()
    expect(screen.getByText('Neo4j unreachable')).toBeInTheDocument()
    expect(screen.getByText('trace-graph-002')).toBeInTheDocument()
  })
})
