import type { ItemPageResponse } from '@/shared/types/api'
import type { ToolInvocationSummary } from '@/features/tools/types'

export type ModelCallStatus = 'SUCCESS' | 'FAILED' | 'TIMEOUT' | 'CANCELLED' | string
export type ModelCallScenario =
  | 'CHAT_SYNC'
  | 'CHAT_STREAM'
  | 'RAG_ANSWER'
  | 'GRAPHRAG_EXTRACTION'
  | 'WORKFLOW_PLANNER'
  | 'WORKFLOW_EXECUTOR'
  | 'WORKFLOW_REVIEWER'
  | string

export type AuditResult = 'SUCCESS' | 'FAILED' | 'DENIED' | string
export type AuditEventType =
  | 'LOGIN'
  | 'LOGOUT'
  | 'TOOL_INVOCATION'
  | 'TOOL_PERMISSION_DENIED'
  | 'WORKFLOW_RUN'
  | 'USER_MANAGEMENT'
  | 'ROLE_MANAGEMENT'
  | string

export type GraphRagIndexStatus = 'PROCESSING' | 'INDEXED' | 'FAILED' | 'SKIPPED'
export type GraphRagRetrievalStatus = 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'DEGRADED'
export type GraphRagRetrievalMode = 'GRAPH_ONLY' | 'HYBRID' | string
export type RagRetrievalStatus = 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'DEGRADED' | string
export type RagRetrievalMode = 'VECTOR_ONLY' | 'GRAPH_ONLY' | 'HYBRID' | string

export interface ModelCall {
  id: string
  traceId: string
  conversationId: string | null
  messageId: string | null
  workflowRunId: string | null
  workflowStepId: string | null
  provider: string | null
  modelName: string | null
  scenario: ModelCallScenario
  promptSummary: string | null
  responseSummary: string | null
  inputTokens: number | null
  outputTokens: number | null
  totalTokens: number | null
  durationMs: number
  status: ModelCallStatus
  errorCode: string | null
  errorMessage: string | null
  createdAt: string
}

export interface AuditLog {
  id: string
  eventType: AuditEventType
  resourceType: string | null
  resourceId: string | null
  userId: string | null
  username: string | null
  result: AuditResult
  durationMs: number | null
  requestSummary: string | null
  responseSummary: string | null
  errorMessage: string | null
  traceId: string | null
  createdAt: string
}

export interface RagRetrievalLog {
  id: string
  traceId: string
  conversationId: string | null
  messageId: string | null
  workflowRunId: string | null
  workflowStepId: string | null
  query: string
  retrievalMode: RagRetrievalMode
  metadataFilter: Record<string, unknown>
  businessDomain: string | null
  documentType: string | null
  permissionLevel: string | null
  timeRange: string | null
  documentId: string | null
  topK: number
  similarityThreshold: number
  matchedChunkIds: string[]
  citationSummaries: Record<string, unknown>[]
  scoreSummary: string | null
  citationCount: number
  durationMs: number
  status: RagRetrievalStatus
  errorMessage: string | null
  startedAt: string
  completedAt: string | null
  createdAt: string
}

export interface GraphRagIndexLog {
  id: string
  documentId: string
  traceId: string
  entityCount: number
  relationshipCount: number
  chunkCount: number
  chunkEntityCount: number
  neo4jEnabled: boolean
  durationMs: number
  status: GraphRagIndexStatus
  errorMessage: string | null
  startedAt: string
  completedAt: string | null
  createdAt: string
}

export interface GraphRagRetrievalLog {
  id: string
  traceId: string
  conversationId: string | null
  messageId: string | null
  workflowRunId: string | null
  workflowStepId: string | null
  query: string
  retrievalMode: GraphRagRetrievalMode
  businessDomain: string | null
  permissionLevel: string | null
  documentId: string | null
  maxDepth: number
  maxPathCount: number
  resolvedEntities: string[]
  matchedPathCount: number
  filteredPathCount: number
  sourceChunkIds: string[]
  confidenceSummary: string | null
  durationMs: number
  status: GraphRagRetrievalStatus
  errorMessage: string | null
  startedAt: string
  completedAt: string | null
  createdAt: string
}

export interface GraphRagSummary {
  latestIndexLog: GraphRagIndexLog | null
  latestRetrievalLog: GraphRagRetrievalLog | null
  indexLogCount: number
  retrievalLogCount: number
}

export interface ModelCallSummary {
  total: number
  failed: number
  timedOut: number
  failureRate: number
  timeoutRate: number
  averageDurationMs: number
  latestCreatedAt: string | null
}

export interface RagSummary {
  total: number
  successful: number
  failed: number
  degraded: number
  failureRate: number
  averageDurationMs: number
  citationCount: number
  latestCreatedAt: string | null
}

export interface WorkflowSummary {
  total: number
  running: number
  succeeded: number
  failed: number
  cancelled: number
  failureRate: number
  averageDurationMs: number
  latestCreatedAt: string | null
}

export interface SseSummary {
  activeConnections: number
  completedConnections: number
  failedConnections: number
  timedOutConnections: number
  averageConnectionDurationMs: number
  averageFirstTokenDurationMs: number
}

export interface HealthSummary {
  status: string
  components: Record<string, string>
  groups: string[]
}

export interface ObservabilitySummary {
  modelCallSummary: ModelCallSummary
  ragSummary: RagSummary
  graphRagSummary: GraphRagSummary
  toolSummary: ToolInvocationSummary
  workflowSummary: WorkflowSummary
  sseSummary: SseSummary
  healthSummary: HealthSummary
}

export interface MetricTrendPoint {
  label: string
  value: number
}

export interface ErrorDistributionItem {
  label: string
  value: number
  color?: string
}

export interface GraphRagIndexLogQuery {
  page: number
  size: number
  documentId?: string
  traceId?: string
  status?: GraphRagIndexStatus
  neo4jEnabled?: boolean
  createdFrom?: string
  createdTo?: string
}

export interface GraphRagRetrievalLogQuery {
  page: number
  size: number
  retrievalMode?: string
  businessDomain?: string
  permissionLevel?: string
  status?: GraphRagRetrievalStatus
  conversationId?: string
  messageId?: string
  workflowRunId?: string
  workflowStepId?: string
  documentId?: string
  traceId?: string
  createdFrom?: string
  createdTo?: string
}

export type GraphRagIndexLogPage = ItemPageResponse<GraphRagIndexLog>
export type GraphRagRetrievalLogPage = ItemPageResponse<GraphRagRetrievalLog>
export type ModelCallPage = ItemPageResponse<ModelCall>
export type AuditLogPage = ItemPageResponse<AuditLog>
export type RagRetrievalLogPage = ItemPageResponse<RagRetrievalLog>

export interface ModelCallQuery {
  page: number
  size: number
  modelName?: string
  scenario?: ModelCallScenario
  status?: ModelCallStatus
  traceId?: string
  createdFrom?: string
  createdTo?: string
}

export interface AuditLogQuery {
  page: number
  size: number
  eventType?: AuditEventType
  result?: AuditResult
  userId?: string
  resourceType?: string
  resourceId?: string
  traceId?: string
  createdFrom?: string
  createdTo?: string
}

export interface RagRetrievalLogQuery {
  page: number
  size: number
  retrievalMode?: string
  businessDomain?: string
  documentType?: string
  permissionLevel?: string
  status?: RagRetrievalStatus
  conversationId?: string
  messageId?: string
  workflowRunId?: string
  workflowStepId?: string
  traceId?: string
  createdFrom?: string
  createdTo?: string
}
