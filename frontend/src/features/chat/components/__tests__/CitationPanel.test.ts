import { render, screen } from '@testing-library/vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'

import CitationPanel from '@/features/chat/components/CitationPanel.vue'

describe('CitationPanel', () => {
  it('shows document, chunk, score, snippet and metadata', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/documents/:documentId',
          name: 'KnowledgeDocumentDetail',
          component: { template: '<div />' },
        },
      ],
    })
    await router.push('/')
    await router.isReady()

    render(CitationPanel, {
      global: {
        plugins: [router],
      },
      props: {
        citations: [
          {
            documentId: 'doc-1',
            documentName: 'Order Runbook',
            chunkId: 'chunk-1',
            title: 'Order Runbook',
            source: 'runbook',
            snippet: 'restart order worker',
            score: 0.92,
            businessDomain: 'order',
            documentType: 'RUNBOOK',
            permissionLevel: 'INTERNAL',
          },
        ],
      },
    })

    expect(screen.getByText('引用资料')).toBeInTheDocument()
    expect(screen.getByText('Order Runbook')).toBeInTheDocument()
    expect(screen.getByText('restart order worker')).toBeInTheDocument()
    expect(screen.getByText('score 0.920')).toBeInTheDocument()
    expect(screen.getByText('chunk-1')).toBeInTheDocument()
    expect(screen.getByText('runbook')).toBeInTheDocument()
    expect(screen.getByText('order')).toBeInTheDocument()
    expect(screen.getByText('RUNBOOK')).toBeInTheDocument()
    expect(screen.getByText('INTERNAL')).toBeInTheDocument()
  })

  it('shows the supplied empty state text', () => {
    render(CitationPanel, {
      props: {
        citations: [],
        emptyText: '本次回答没有返回引用资料',
      },
    })

    expect(screen.getByText('本次回答没有返回引用资料')).toBeInTheDocument()
  })
})
