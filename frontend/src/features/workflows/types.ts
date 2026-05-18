import type { ItemPageResponse } from '@/shared/types/api'

export type WorkflowRunStatus =
  | 'CREATED'
  | 'PLANNING'
  | 'EXECUTING'
  | 'WAITING_APPROVAL'
  | 'REVIEWING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'CANCELLED'

export type WorkflowStepType =
  | 'PLANNING'
  | 'RAG_SEARCH'
  | 'GRAPH_RAG_SEARCH'
  | 'TOOL_CALL'
  | 'REVIEW'
  | 'FINAL_ANSWER'
  | 'HUMAN_APPROVAL'
  | 'CHECKPOINT'
  | 'ERROR'

export type WorkflowStepStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'WAITING_APPROVAL'
  | 'RETRYING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'SKIPPED'
export type WorkflowAgentRole = 'PLANNER' | 'EXECUTOR' | 'REVIEWER' | 'APPROVAL' | 'SYSTEM'
export type WorkflowApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
export type ToolRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'

export interface WorkflowRun {
  runId: string
  conversationId: string | null
  userId: string
  goal: string
  status: WorkflowRunStatus
  currentStepIndex: number
  finalAnswer: string | null
  errorCode?: string | null
  errorMessage?: string | null
  traceId: string | null
  startedAt: string | null
  finishedAt: string | null
  createdAt: string
  updatedAt?: string | null
}

export interface WorkflowStep {
  stepId: string
  stepIndex: number
  stepType: WorkflowStepType
  nodeName: string | null
  agentRole: WorkflowAgentRole | null
  traceId: string | null
  status: WorkflowStepStatus
  inputSummary: string | null
  outputSummary: string | null
  startedAt: string | null
  finishedAt: string | null
  durationMs: number | null
  retryCount: number
  retryReason: string | null
  lastRetriedAt: string | null
  errorCode: string | null
  errorMessage: string | null
  citations?: WorkflowCitation[]
  graphPaths?: WorkflowGraphPath[]
  toolCalls?: WorkflowToolCall[]
}

export interface WorkflowApproval {
  approvalId: string
  runId: string
  stepId: string
  stepIndex: number
  toolCode: string
  riskLevel: ToolRiskLevel
  requestSummary: string | null
  status: WorkflowApprovalStatus
  requestedBy: string
  approvedBy: string | null
  decisionReason: string | null
  createdAt: string
  decidedAt: string | null
}

export interface WorkflowCheckpoint {
  checkpointId: string
  runId: string
  stepIndex: number
  nodeName: string | null
  stateVersion: number
  checksum: string | null
  recoverable: boolean
  errorCode: string | null
  errorMessage: string | null
  createdAt: string
}

export interface WorkflowCitation {
  documentId?: string | null
  documentName?: string | null
  chunkId?: string | null
  title?: string | null
  source?: string | null
  snippet: string
  score?: number | null
}

export interface WorkflowGraphPath {
  pathId?: string | null
  depth: number
  entities: string[]
  relationships: string[]
  sourceChunkIds: string[]
  confidence?: number | null
}

export interface WorkflowToolCall {
  toolCode: string
  status: string
  inputSummary?: string | null
  resultSummary?: string | null
  latencyMs?: number | null
  traceId?: string | null
}

export interface WorkflowRunQuery {
  page: number
  size: number
  status?: WorkflowRunStatus
}

export interface CreateWorkflowRunPayload {
  conversationId?: string | null
  goal: string
}

export interface WorkflowApprovalQuery {
  status?: WorkflowApprovalStatus
}

export interface ApprovalDecisionPayload {
  reason?: string
}

export type WorkflowRunPage = ItemPageResponse<WorkflowRun>
