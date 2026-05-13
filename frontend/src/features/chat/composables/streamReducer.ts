import type { ChatStreamEvent, ChatStreamState, ToolInvocation } from '@/features/chat/types'

export function createInitialStreamState(): ChatStreamState {
  return {
    status: 'idle',
    assistantMessageId: null,
    content: '',
    seenEventIds: [],
    citations: [],
    graphPaths: [],
    toolInvocations: [],
    workflowSteps: [],
    error: null,
  }
}

export function reduceStreamEvent(state: ChatStreamState, event: ChatStreamEvent): ChatStreamState {
  if (event.eventId && state.seenEventIds.includes(event.eventId)) {
    return state
  }

  const nextState = event.eventId
    ? { ...state, seenEventIds: [...state.seenEventIds, event.eventId] }
    : state

  if (nextState.status === 'completed' && event.type !== 'error') {
    return nextState
  }

  switch (event.type) {
    case 'message_delta':
      return {
        ...nextState,
        status: 'streaming',
        content: `${nextState.content}${event.delta}`,
      }
    case 'tool_call_started':
      return {
        ...nextState,
        status: 'tool_calling',
        toolInvocations: upsertToolInvocation(nextState.toolInvocations, {
          toolCallId: event.toolCallId,
          toolName: event.toolName,
          inputSummary: event.inputSummary,
          status: 'RUNNING',
          traceId: event.traceId,
        }),
      }
    case 'tool_call_finished':
      return {
        ...nextState,
        status: 'streaming',
        toolInvocations: upsertToolInvocation(nextState.toolInvocations, {
          toolCallId: event.toolCallId,
          toolName: findToolName(nextState.toolInvocations, event.toolCallId),
          status: event.status,
          resultSummary: event.resultSummary,
          latencyMs: event.latencyMs,
          traceId: event.traceId,
        }),
      }
    case 'citation':
      return {
        ...nextState,
        citations: [
          ...nextState.citations,
          {
            documentId: event.documentId,
            documentName: event.documentName,
            chunkId: event.chunkId,
            title: event.title,
            source: event.source,
            snippet: event.snippet,
            score: event.score,
          },
        ],
      }
    case 'graph_path':
      return {
        ...nextState,
        graphPaths: upsertGraphPath(nextState.graphPaths, event.graphPath),
      }
    case 'workflow_step':
      return {
        ...nextState,
        workflowSteps: [
          ...nextState.workflowSteps.filter((step) => step.workflowRunId !== event.workflowRunId),
          {
            workflowRunId: event.workflowRunId,
            stepName: event.stepName,
            status: event.status,
            traceId: event.traceId,
          },
        ],
      }
    case 'done':
      return {
        ...nextState,
        status: 'completed',
        assistantMessageId: event.messageId,
      }
    case 'error':
      return {
        ...nextState,
        status: 'failed',
        error: {
          code: event.code,
          message: event.message,
          traceId: event.traceId,
        },
      }
  }
}

function upsertToolInvocation(items: ToolInvocation[], next: ToolInvocation): ToolInvocation[] {
  const exists = items.some((item) => item.toolCallId === next.toolCallId)
  if (!exists) {
    return [...items, next]
  }

  return items.map((item) => (item.toolCallId === next.toolCallId ? { ...item, ...next } : item))
}

function findToolName(items: ToolInvocation[], toolCallId: string): string {
  return items.find((item) => item.toolCallId === toolCallId)?.toolName ?? 'unknown'
}

function upsertGraphPath<T extends { pathId?: string }>(items: T[], next: T): T[] {
  if (!next.pathId) {
    return [...items, next]
  }

  const exists = items.some((item) => item.pathId === next.pathId)
  if (!exists) {
    return [...items, next]
  }

  return items.map((item) => (item.pathId === next.pathId ? next : item))
}
