import type { DocumentChunkStatus, DocumentStatus } from '@/features/knowledge/types'

export type StatusTagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

export interface StatusMeta {
  label: string
  type: StatusTagType
}

export const documentStatusMeta: Record<DocumentStatus, StatusMeta> = {
  uploaded: { label: '已上传', type: 'info' },
  parsing: { label: '解析中', type: 'primary' },
  cleaning: { label: '清洗中', type: 'primary' },
  chunking: { label: '分段中', type: 'primary' },
  embedding: { label: '向量化中', type: 'warning' },
  indexed: { label: '已入库', type: 'success' },
  failed: { label: '失败', type: 'danger' },
}

export const documentChunkStatusMeta: Record<DocumentChunkStatus, StatusMeta> = {
  pending_embedding: { label: '待向量化', type: 'info' },
  embedding: { label: '向量化中', type: 'warning' },
  indexed: { label: '已入库', type: 'success' },
  failed: { label: '失败', type: 'danger' },
}
