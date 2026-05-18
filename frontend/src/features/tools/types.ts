import type { ItemPageResponse } from '@/shared/types/api'

export type ToolRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'
export type ToolType =
  | 'BUSINESS_QUERY'
  | 'LOG_SEARCH'
  | 'DATABASE_READ'
  | 'ENDPOINT_STATUS'
  | 'NOTIFICATION'
  | 'MCP_RESOURCE'
  | 'SCRIPT'
  | 'UNKNOWN'

export interface ToolDefinition {
  id: string
  code: string
  name: string
  toolType: ToolType
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
  toolName: string
  toolType: ToolType
  riskLevel: ToolRiskLevel | null
  userId: string
  username: string
  conversationId: string | null
  messageId: string | null
  workflowRunId: string | null
  workflowStepId: string | null
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
  toolType?: ToolType
  status?: ToolInvocationStatus
  createdFrom?: string
  createdTo?: string
}

export type ToolInvocationPage = ItemPageResponse<ToolInvocation>

export interface ToolInvocationStatusCount {
  status: ToolInvocationStatus
  count: number
}

export interface ToolInvocationToolCount {
  toolCode: string
  toolName: string
  toolType: ToolType
  count: number
  failed: number
  denied: number
  timeout: number
  averageDurationMs: number
}

export interface ToolInvocationSummary {
  total: number
  running: number
  success: number
  failed: number
  denied: number
  timeout: number
  failureRate: number
  deniedRate: number
  timeoutRate: number
  averageDurationMs: number
  statusCounts: ToolInvocationStatusCount[]
  toolCounts: ToolInvocationToolCount[]
  invocations: ToolInvocationPage
}
