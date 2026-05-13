import { describe, expect, it } from 'vitest'

import {
  documentChunkStatusMeta,
  documentStatusMeta,
} from '@/features/knowledge/components/documentStatus'

describe('documentStatusMeta', () => {
  it('maps document statuses to stable labels', () => {
    expect(documentStatusMeta.uploaded).toEqual({ label: '已上传', type: 'info' })
    expect(documentStatusMeta.failed).toEqual({ label: '失败', type: 'danger' })
  })

  it('maps chunk statuses to stable labels', () => {
    expect(documentChunkStatusMeta.pending_embedding).toEqual({ label: '待向量化', type: 'info' })
    expect(documentChunkStatusMeta.indexed).toEqual({ label: '已入库', type: 'success' })
  })
})
