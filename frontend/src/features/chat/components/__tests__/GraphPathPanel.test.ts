import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import GraphPathPanel from '@/features/chat/components/GraphPathPanel.vue'

describe('GraphPathPanel', () => {
  it('shows graph path entities, relationships, chunks and confidence', () => {
    render(GraphPathPanel, {
      props: {
        graphPaths: [
          {
            pathId: 'path-1',
            depth: 2,
            entities: ['order-service', 'payment-api'],
            relationships: ['CALLS'],
            sourceChunkIds: ['chunk-1'],
            confidence: 0.78,
          },
        ],
      },
    })

    expect(screen.getByText('图谱路径')).toBeInTheDocument()
    expect(screen.getByText('path-1')).toBeInTheDocument()
    expect(screen.getByText('order-service')).toBeInTheDocument()
    expect(screen.getByText('payment-api')).toBeInTheDocument()
    expect(screen.getByText('CALLS')).toBeInTheDocument()
    expect(screen.getByText('chunks: chunk-1')).toBeInTheDocument()
    expect(screen.getByText('confidence: 0.780')).toBeInTheDocument()
  })

  it('shows an empty state', () => {
    render(GraphPathPanel, {
      props: {
        graphPaths: [],
      },
    })

    expect(screen.getByText('暂无图谱路径')).toBeInTheDocument()
  })
})
