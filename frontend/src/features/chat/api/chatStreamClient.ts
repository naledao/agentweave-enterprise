import { getAuthToken } from '@/features/auth/store/authSession'
import type {
  ChatStreamEvent,
  LegacyChatStreamEvent,
  ToolInvocationStatus,
  WorkflowStepStatus,
} from '@/features/chat/types'
import { createRequestId } from '@/shared/utils/requestId'

interface ChatStreamHandlers {
  onEvent: (event: ChatStreamEvent) => void
  onError: (event: ChatStreamEvent) => void
}

interface ChatStreamConnection {
  close: () => void
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'

export function openChatStream(conversationId: string, handlers: ChatStreamHandlers): ChatStreamConnection {
  const controller = new AbortController()
  void consumeStream(conversationId, controller.signal, handlers)

  return {
    close: () => controller.abort(),
  }
}

async function consumeStream(
  conversationId: string,
  signal: AbortSignal,
  handlers: ChatStreamHandlers,
): Promise<void> {
  try {
    const response = await fetch(`${apiBaseUrl}/conversations/${conversationId}/stream`, {
      method: 'GET',
      signal,
      headers: buildHeaders(),
    })

    if (!response.ok || !response.body) {
      handlers.onError({
        type: 'error',
        code: `HTTP_${response.status}`,
        message: 'SSE 连接失败',
        traceId: response.headers.get('x-trace-id') ?? undefined,
      })
      return
    }

    await readEventStream(response.body, handlers.onEvent)
  } catch (error) {
    if (signal.aborted) {
      return
    }

    handlers.onError({
      type: 'error',
      code: 'SSE_CONNECTION_ERROR',
      message: error instanceof Error ? error.message : 'SSE 连接异常',
    })
  }
}

function buildHeaders(): HeadersInit {
  const token = getAuthToken()
  const headers: Record<string, string> = {
    Accept: 'text/event-stream',
    'X-Request-Id': createRequestId(),
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  return headers
}

async function readEventStream(stream: ReadableStream<Uint8Array>, onEvent: (event: ChatStreamEvent) => void): Promise<void> {
  const reader = stream.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })
    const chunks = buffer.split(/\r?\n\r?\n/)
    buffer = chunks.pop() ?? ''

    for (const chunk of chunks) {
      const event = parseSseChunk(chunk)
      if (event) {
        onEvent(event)
      }
    }
  }

  const remaining = parseSseChunk(buffer)
  if (remaining) {
    onEvent(remaining)
  }
}

function parseSseChunk(chunk: string): ChatStreamEvent | null {
  const lines = chunk.split(/\r?\n/)
  const eventType = lines.find((line) => line.startsWith('event:'))?.slice(6).trim()
  const data = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trimStart())
    .join('\n')

  if (!eventType || !data) {
    return null
  }

  return normalizeStreamEvent(eventType, JSON.parse(data) as Record<string, unknown>)
}

export function normalizeStreamEvent(eventType: string, data: Record<string, unknown>): ChatStreamEvent | null {
  const typed = { type: eventType, ...data } as ChatStreamEvent | LegacyChatStreamEvent
  switch (typed.type) {
    case 'message_delta':
      return {
        ...baseFields(data),
        type: 'message_delta',
        delta: stringField(data.delta ?? data.content),
      }
    case 'tool_call_started':
      return {
        ...baseFields(data),
        type: 'tool_call_started',
        toolCallId: stringField(data.toolCallId ?? data.invocationId),
        toolName: stringField(data.toolName),
        inputSummary: optionalStringField(data.inputSummary),
      }
    case 'tool_call_finished':
      return {
        ...baseFields(data),
        type: 'tool_call_finished',
        toolCallId: stringField(data.toolCallId ?? data.invocationId),
        status: normalizeExecutionStatus(data.status),
        latencyMs: optionalNumberField(data.latencyMs ?? data.elapsedMs),
        resultSummary: optionalStringField(data.resultSummary ?? data.summary),
      }
    case 'citation': {
      const citation = isRecord(data.citation) ? data.citation : data
      return {
        ...baseFields(data),
        type: 'citation',
        documentId: optionalStringField(citation.documentId),
        documentName: optionalStringField(citation.documentName),
        chunkId: optionalStringField(citation.chunkId),
        source: optionalStringField(citation.source),
        title: stringField(citation.title ?? citation.documentTitle),
        snippet: stringField(citation.snippet),
        score: optionalNumberField(citation.score),
        businessDomain: optionalStringField(citation.businessDomain),
        documentType: optionalStringField(citation.documentType),
        permissionLevel: optionalStringField(citation.permissionLevel),
      }
    }
    case 'graph_path': {
      const graphPath = isRecord(data.graphPath) ? data.graphPath : data
      return {
        ...baseFields(data),
        type: 'graph_path',
        graphPath: {
          pathId: optionalStringField(graphPath.pathId),
          depth: numberField(graphPath.depth),
          entities: stringArrayField(graphPath.entities),
          relationships: stringArrayField(graphPath.relationships),
          sourceChunkIds: stringArrayField(graphPath.sourceChunkIds),
          confidence: optionalNumberField(graphPath.confidence),
        },
      }
    }
    case 'workflow_step': {
      const step = isRecord(data.step) ? data.step : data
      return {
        ...baseFields(data),
        type: 'workflow_step',
        workflowRunId: stringField(step.workflowRunId ?? step.stepId),
        stepName: stringField(step.stepName ?? step.name),
        status: normalizeWorkflowStatus(step.status),
      }
    }
    case 'done':
      return {
        ...baseFields(data),
        type: 'done',
        messageId: stringField(data.messageId),
        status: optionalStringField(data.status) as 'SUCCEEDED' | 'FAILED' | undefined,
      }
    case 'error':
      return {
        ...baseFields(data),
        type: 'error',
        code: stringField(data.code),
        message: stringField(data.message),
      }
    default:
      return null
  }
}

function baseFields(data: Record<string, unknown>) {
  return {
    eventId: optionalStringField(data.eventId),
    conversationId: optionalStringField(data.conversationId),
    messageId: optionalStringField(data.messageId),
    traceId: optionalStringField(data.traceId),
    timestamp: optionalStringField(data.timestamp),
    createdAt: optionalStringField(data.createdAt ?? data.timestamp),
  }
}

function normalizeExecutionStatus(value: unknown): ToolInvocationStatus {
  const status = stringField(value)
  return status === 'SUCCESS' ? 'SUCCEEDED' : status as ToolInvocationStatus
}

function normalizeWorkflowStatus(value: unknown): WorkflowStepStatus {
  const status = stringField(value)
  return status === 'SUCCESS' ? 'SUCCEEDED' : status as WorkflowStepStatus
}

function stringField(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

function optionalStringField(value: unknown): string | undefined {
  return typeof value === 'string' && value ? value : undefined
}

function optionalNumberField(value: unknown): number | undefined {
  return typeof value === 'number' ? value : undefined
}

function numberField(value: unknown): number {
  return typeof value === 'number' ? value : 0
}

function stringArrayField(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : []
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
