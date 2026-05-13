import { httpClient } from '@/shared/api/httpClient'
import type {
  DocumentPage,
  DocumentQuery,
  KnowledgeDocument,
  KnowledgeDocumentDetail,
  KnowledgeDocumentUploadPayload,
} from '@/features/knowledge/types'

type UploadProgressHandler = (progress: number) => void

export const knowledgeDocumentApi = {
  async upload(
    payload: KnowledgeDocumentUploadPayload,
    onUploadProgress?: UploadProgressHandler,
  ): Promise<KnowledgeDocument> {
    const formData = new FormData()
    formData.append('file', payload.file)
    formData.append('source', payload.source)
    formData.append('businessDomain', payload.businessDomain)
    formData.append('documentType', payload.documentType)
    formData.append('permissionLevel', payload.permissionLevel)

    if (payload.effectiveFrom) {
      formData.append('effectiveFrom', payload.effectiveFrom)
    }
    if (payload.effectiveTo) {
      formData.append('effectiveTo', payload.effectiveTo)
    }
    payload.tags.forEach((tag) => {
      formData.append('tags', tag)
    })

    const { data } = await httpClient.post<KnowledgeDocument>('/documents', formData, {
      onUploadProgress(event) {
        if (!onUploadProgress || !event.total) {
          return
        }
        onUploadProgress(Math.round((event.loaded / event.total) * 100))
      },
    })
    return data
  },

  async list(params: DocumentQuery): Promise<DocumentPage> {
    const { data } = await httpClient.get<DocumentPage>('/documents', { params })
    return data
  },

  async detail(documentId: string): Promise<KnowledgeDocumentDetail> {
    const { data } = await httpClient.get<KnowledgeDocumentDetail>(`/documents/${documentId}`)
    return data
  },

  async parseDocument(documentId: string): Promise<KnowledgeDocument> {
    const { data } = await httpClient.post<KnowledgeDocument>(`/documents/${documentId}/parse`)
    return data
  },

  async delete(documentId: string): Promise<void> {
    await httpClient.delete(`/documents/${documentId}`)
  },

  async reindex(documentId: string): Promise<KnowledgeDocument> {
    const { data } = await httpClient.post<KnowledgeDocument>(`/documents/${documentId}/reindex`)
    return data
  },

  async queryDocuments(params: DocumentQuery): Promise<DocumentPage> {
    return knowledgeDocumentApi.list(params)
  },

  async getDocument(documentId: string): Promise<KnowledgeDocumentDetail> {
    return knowledgeDocumentApi.detail(documentId)
  },

  async reindexDocument(documentId: string): Promise<KnowledgeDocument> {
    return knowledgeDocumentApi.reindex(documentId)
  },
}

export const documentsApi = knowledgeDocumentApi
