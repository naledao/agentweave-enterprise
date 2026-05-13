import { afterEach, describe, expect, it, vi } from 'vitest'

import { knowledgeDocumentApi } from '@/features/knowledge/api/documentsApi'
import { httpClient } from '@/shared/api/httpClient'

describe('knowledgeDocumentApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('builds multipart payload for upload', async () => {
    const post = vi.spyOn(httpClient, 'post').mockResolvedValue({
      data: {
        documentId: 'document-1',
      },
    } as never)

    const file = new File(['hello'], 'runbook.txt', { type: 'text/plain' })

    await knowledgeDocumentApi.upload(
      {
        file,
        source: 'runbook',
        businessDomain: 'order',
        documentType: 'RUNBOOK',
        permissionLevel: 'INTERNAL',
        effectiveFrom: '2026-05-01T08:00:00.000Z',
        effectiveTo: null,
        tags: ['api', 'runbook'],
      } as never,
    )

    const [, formData] = post.mock.calls[0] as [string, FormData]
    expect(formData.get('file')).toBe(file)
    expect(formData.get('source')).toBe('runbook')
    expect(formData.get('businessDomain')).toBe('order')
    expect(formData.get('documentType')).toBe('RUNBOOK')
    expect(formData.get('permissionLevel')).toBe('INTERNAL')
    expect(formData.get('effectiveFrom')).toBe('2026-05-01T08:00:00.000Z')
    expect(formData.getAll('tags')).toEqual(['api', 'runbook'])
  })

  it('calls delete endpoint', async () => {
    const del = vi.spyOn(httpClient, 'delete').mockResolvedValue({} as never)

    await knowledgeDocumentApi.delete('document-1')

    expect(del).toHaveBeenCalledWith('/documents/document-1')
  })
})
