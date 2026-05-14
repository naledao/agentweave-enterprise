import type { ItemPageResponse } from '@/shared/types/api'

export type ToolRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'

export interface ToolDefinition {
  id: string
  code: string
  name: string
  description: string | null
  permissionCode: string
  riskLevel: ToolRiskLevel
  enabled: boolean
  available: boolean
  inputSchema: string | null
  outputSchema: string | null
  createdAt: string
  updatedAt: string
}

export type ToolInvocationStatus = 'running' | 'success' | 'failed' | 'denied' | 'timeout'

export interface ToolInvocation {
  id: string
  toolCode: string
  userId: string
  username: string
  conversationId: string | null
  messageId: string | null
  inputSummary: string | null
  resultSummary: string | null
  status: ToolInvocationStatus
  durationMs: number | null
  errorMessage: string | null
  traceId: string | null
  createdAt: string
  finishedAt: string | null
}

export type ToolInvocationDetail = ToolInvocation

export interface ToolInvocationQuery {
  page: number
  size: number
  toolCode?: string
  status?: ToolInvocationStatus
  createdFrom?: string
  createdTo?: string
}

export type ToolInvocationPage = ItemPageResponse<ToolInvocation>
