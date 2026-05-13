import { describe, expect, it } from 'vitest'

import { normalizeStreamEvent } from '@/features/chat/api/chatStreamClient'

describe('chatStreamClient', () => {
  it('normalizes citation events with metadata fields', () => {
    const event = normalizeStreamEvent('citation', {
      eventId: 'evt-citation-1',
      traceId: 'trace-1',
      citation: {
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
    })

    expect(event).toEqual({
      type: 'citation',
      eventId: 'evt-citation-1',
      traceId: 'trace-1',
      conversationId: undefined,
      messageId: undefined,
      timestamp: undefined,
      createdAt: undefined,
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
    })
  })

  it('normalizes graph_path events with common correlation fields', () => {
    const event = normalizeStreamEvent('graph_path', {
      eventId: 'evt-graph-1',
      traceId: 'trace-1',
      conversationId: 'conversation-1',
      messageId: 'assistant-1',
      timestamp: '2026-05-13T08:00:00Z',
      createdAt: '2026-05-13T08:00:00Z',
      graphPath: {
        pathId: 'path-1',
        depth: 2,
        entities: ['order-service', 'payment-api'],
        relationships: ['CALLS'],
        sourceChunkIds: ['chunk-1'],
        confidence: 0.78,
      },
    })

    expect(event).toEqual({
      type: 'graph_path',
      eventId: 'evt-graph-1',
      traceId: 'trace-1',
      conversationId: 'conversation-1',
      messageId: 'assistant-1',
      timestamp: '2026-05-13T08:00:00Z',
      createdAt: '2026-05-13T08:00:00Z',
      graphPath: {
        pathId: 'path-1',
        depth: 2,
        entities: ['order-service', 'payment-api'],
        relationships: ['CALLS'],
        sourceChunkIds: ['chunk-1'],
        confidence: 0.78,
      },
    })
  })

  it('normalizes flat graph_path payloads for compatibility', () => {
    const event = normalizeStreamEvent('graph_path', {
      pathId: 'path-flat',
      depth: 1,
      entities: ['api-gateway', 'order-service'],
      relationships: ['CALLS'],
      sourceChunkIds: ['chunk-flat'],
    })

    expect(event).toMatchObject({
      type: 'graph_path',
      graphPath: {
        pathId: 'path-flat',
        depth: 1,
        entities: ['api-gateway', 'order-service'],
        relationships: ['CALLS'],
        sourceChunkIds: ['chunk-flat'],
      },
    })
  })
})
