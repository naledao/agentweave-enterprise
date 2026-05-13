import type { ItemPageResponse } from '@/shared/types/api'

export type DocumentStatus =
  | 'uploaded'
  | 'parsing'
  | 'cleaning'
  | 'chunking'
  | 'embedding'
  | 'indexed'
  | 'failed'

export type DocumentChunkStatus =
  | 'pending_embedding'
  | 'embedding'
  | 'indexed'
  | 'failed'

export interface DocumentStorageSummary {
  bucket: string
  objectKey: string
  checksum: string
}

export interface KnowledgeDocumentMetadata {
  source: string
  businessDomain: string
  documentType: string
  permissionLevel: string
  effectiveFrom: string | null
  effectiveTo: string | null
  tags: string[]
}

export interface DocumentMetadataFormModel {
  source: string
  businessDomain: string
  documentType: string
  permissionLevel: string
  effectiveFrom: Date | null
  effectiveTo: Date | null
  tags: string[]
}

export interface KnowledgeDocumentUploadPayload extends KnowledgeDocumentMetadata {
  file: File
}

export interface KnowledgeDocument {
  documentId: string
  filename: string
  contentType: string
  fileSize: number
  uploadedBy: string
  status: DocumentStatus
  errorMessage: string | null
  traceId: string | null
  chunkCount: number
  reindexCount: number
  indexedAt: string | null
  storage: DocumentStorageSummary
  metadata: KnowledgeDocumentMetadata
  createdAt: string
  updatedAt: string
}

export interface DocumentChunk {
  chunkId: string
  chunkIndex: number
  content: string
  contentLength: number
  status: DocumentChunkStatus
  errorMessage: string | null
  createdAt: string
  updatedAt: string
}

export type GraphRagIndexStatus =
  | 'pending'
  | 'processing'
  | 'indexed'
  | 'failed'

export interface GraphRagIndexSummaryResponse {
  status: GraphRagIndexStatus
  entityCount: number
  relationshipCount: number
  chunkCount: number
  errorMessage: string | null
  traceId: string | null
  indexedAt: string | null
}

export interface KnowledgeDocumentDetail extends KnowledgeDocument {
  graphRag: GraphRagIndexSummaryResponse
  chunks: DocumentChunk[]
  citationRecords: DocumentCitationRecord[]
}

export interface DocumentCitationRecord {
  conversationId: string
  messageId: string
  messagePreview: string
  traceId: string | null
  createdAt: string
}

export interface DocumentQuery {
  page: number
  size: number
  keyword?: string
}

export type DocumentPage = ItemPageResponse<KnowledgeDocument>
