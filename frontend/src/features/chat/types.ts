export type ConversationStatus = 'ACTIVE' | 'ARCHIVED' | 'DELETED'
export type ChatRole = 'USER' | 'ASSISTANT' | 'SYSTEM' | 'TOOL'
export type ChatMessageStatus = 'PENDING' | 'SUCCEEDED' | 'STREAMING' | 'FAILED' | 'CANCELLED'
export type StreamStatus = 'idle' | 'connecting' | 'streaming' | 'tool_calling' | 'completed' | 'failed' | 'cancelled'
export type ToolInvocationStatus = 'RUNNING' | 'SUCCEEDED' | 'FAILED'
export type WorkflowStepStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'SKIPPED'

export interface ConversationSummary {
  id: string
  title: string
  status: ConversationStatus
  messageCount: number
  lastMessagePreview?: string | null
  lastMessageAt?: string | null
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: string
  conversationId: string
  role: ChatRole
  content: string
  status: ChatMessageStatus
  errorCode?: string | null
  errorMessage?: string | null
  metadata?: string | null
  traceId?: string | null
  citations: Citation[]
  toolCalls: ToolInvocation[]
  createdAt: string
}

export interface ConversationDetail extends ConversationSummary {
  messages: ChatMessage[]
  messagePage: number
  messageSize: number
  messageTotal: number
  messageTotalPages: number
  traceId: string
}

export interface ConversationCreateResponse {
  id: string
  title: string
  status: ConversationStatus
  messageCount: number
  createdAt: string
  updatedAt: string
  traceId: string
}

export interface CreateConversationRequest {
  title?: string
}

export interface SendMessageRequest {
  content: string
  responseMode?: 'SYNC' | 'STREAM'
}

export interface SendMessageResponse {
  conversationId: string
  userMessageId: string
  assistantMessageId: string
  traceId: string
}

export interface CancelMessageResponse {
  conversationId: string
  messageId: string
  status: ChatMessageStatus
  traceId: string
}

export interface Citation {
  documentId?: string
  chunkId?: string
  title: string
  snippet: string
  score?: number
}

export interface ToolInvocation {
  toolCallId: string
  toolName: string
  status: ToolInvocationStatus
  inputSummary?: string
  resultSummary?: string
  latencyMs?: number
  traceId?: string
}

export interface WorkflowStep {
  workflowRunId: string
  stepName: string
  status: WorkflowStepStatus
  traceId?: string
}

export type ChatStreamEvent =
  | StreamMessageDeltaEvent
  | StreamToolCallStartedEvent
  | StreamToolCallFinishedEvent
  | StreamCitationEvent
  | StreamWorkflowStepEvent
  | StreamDoneEvent
  | StreamErrorEvent

export interface StreamBaseEvent {
  type: string
  eventId?: string
  conversationId?: string
  messageId?: string
  traceId?: string
  timestamp?: string
}

export interface StreamMessageDeltaEvent extends StreamBaseEvent {
  type: 'message_delta'
  delta: string
}

export interface StreamToolCallStartedEvent extends StreamBaseEvent {
  type: 'tool_call_started'
  toolCallId: string
  toolName: string
  inputSummary?: string
}

export interface StreamToolCallFinishedEvent extends StreamBaseEvent {
  type: 'tool_call_finished'
  toolCallId: string
  status: ToolInvocationStatus
  latencyMs?: number
  resultSummary?: string
}

export interface StreamCitationEvent extends StreamBaseEvent {
  type: 'citation'
  documentId?: string
  chunkId?: string
  title: string
  snippet: string
  score?: number
}

export interface StreamWorkflowStepEvent extends StreamBaseEvent {
  type: 'workflow_step'
  workflowRunId: string
  stepName: string
  status: WorkflowStepStatus
}

export interface StreamDoneEvent extends StreamBaseEvent {
  type: 'done'
  messageId: string
  status?: 'SUCCEEDED' | 'FAILED'
}

export interface StreamErrorEvent extends StreamBaseEvent {
  type: 'error'
  code: string
  message: string
}

interface LegacyCitation {
  id?: string
  documentId?: string
  documentTitle?: string
  title: string
  snippet: string
  score?: number
}

interface LegacyWorkflowStep {
  stepId?: string
  workflowRunId?: string
  name?: string
  stepName?: string
  status: WorkflowStepStatus | 'SUCCESS'
  summary?: string
}

export type LegacyChatStreamEvent =
  | { type: 'message_delta'; content: string }
  | { type: 'tool_call_started'; invocationId: string; toolName: string; inputSummary?: string }
  | {
      type: 'tool_call_finished'
      invocationId: string
      status: ToolInvocationStatus | 'SUCCESS'
      summary?: string
      resultSummary?: string
      elapsedMs?: number
      latencyMs?: number
    }
  | { type: 'citation'; citation: LegacyCitation }
  | { type: 'workflow_step'; step: LegacyWorkflowStep }
  | { type: 'done'; messageId: string }
  | { type: 'error'; code: string; message: string; traceId?: string }

export interface ChatStreamState {
  status: StreamStatus
  assistantMessageId: string | null
  content: string
  seenEventIds: string[]
  citations: Citation[]
  toolInvocations: ToolInvocation[]
  workflowSteps: WorkflowStep[]
  error: {
    code: string
    message: string
    traceId?: string
  } | null
}
