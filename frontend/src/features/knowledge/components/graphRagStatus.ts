import type { StatusMeta, StatusTagType } from '@/features/knowledge/components/documentStatus'
import type { GraphRagIndexStatus } from '@/features/knowledge/types'

export interface GraphRagStatusMeta extends StatusMeta {
  description: string
}

export const graphRagIndexStatusMeta: Record<GraphRagIndexStatus, GraphRagStatusMeta> = {
  pending: {
    label: '待构建',
    type: 'info',
    description: '尚未生成图谱索引',
  },
  processing: {
    label: '构建中',
    type: 'warning',
    description: '实体、关系和 chunk 关联正在抽取',
  },
  indexed: {
    label: '已完成',
    type: 'success',
    description: '图谱索引可用于 GraphRAG 检索',
  },
  failed: {
    label: '失败',
    type: 'danger',
    description: '最近一次图谱构建失败',
  },
}

export function fallbackGraphRagStatusMeta(status: string): GraphRagStatusMeta {
  return {
    label: status,
    type: 'info' as StatusTagType,
    description: '未知状态',
  }
}
