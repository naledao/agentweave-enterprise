import { describe, expect, it } from 'vitest'

import { createInitialStreamState, reduceStreamEvent } from '@/features/chat/composables/streamReducer'

describe('streamReducer', () => {
  it('appends message deltas and marks done', () => {
    let state = createInitialStreamState()

    state = reduceStreamEvent(state, { type: 'message_delta', eventId: 'evt-1', delta: '你好' })
    state = reduceStreamEvent(state, { type: 'message_delta', eventId: 'evt-2', delta: '，已收到。' })
    state = reduceStreamEvent(state, { type: 'done', eventId: 'evt-3', messageId: 'assistant-1' })

    expect(state.content).toBe('你好，已收到。')
    expect(state.status).toBe('completed')
    expect(state.assistantMessageId).toBe('assistant-1')
  })

  it('tracks tool invocation lifecycle with protocol field names', () => {
    let state = createInitialStreamState()

    state = reduceStreamEvent(state, {
      type: 'tool_call_started',
      eventId: 'evt-tool-start',
      toolCallId: 'tool-1',
      toolName: 'ticketQuery',
      inputSummary: '查询最近工单',
    })
    state = reduceStreamEvent(state, {
      type: 'tool_call_finished',
      eventId: 'evt-tool-finish',
      toolCallId: 'tool-1',
      status: 'SUCCEEDED',
      resultSummary: '查询到 2 条工单',
      latencyMs: 42,
    })

    expect(state.toolInvocations).toEqual([
      {
        toolCallId: 'tool-1',
        toolName: 'ticketQuery',
        status: 'SUCCEEDED',
        inputSummary: '查询最近工单',
        resultSummary: '查询到 2 条工单',
        latencyMs: 42,
      },
    ])
  })

  it('stores citations, workflow steps and visible stream errors', () => {
    let state = createInitialStreamState()

    state = reduceStreamEvent(state, {
      type: 'citation',
      eventId: 'evt-citation',
      documentId: 'doc-1',
      chunkId: 'chunk-1',
      title: '排障手册',
      snippet: '先检查服务健康状态。',
      score: 0.91,
    })
    state = reduceStreamEvent(state, {
      type: 'workflow_step',
      eventId: 'evt-step',
      workflowRunId: 'run-1',
      stepName: 'Planner',
      status: 'SUCCEEDED',
      traceId: 'trace-1',
    })
    state = reduceStreamEvent(state, {
      type: 'error',
      eventId: 'evt-error',
      code: 'MODEL_TIMEOUT',
      message: '模型响应超时',
      traceId: 'trace-1',
    })

    expect(state.citations).toHaveLength(1)
    expect(state.workflowSteps).toEqual([
      {
        workflowRunId: 'run-1',
        stepName: 'Planner',
        status: 'SUCCEEDED',
        traceId: 'trace-1',
      },
    ])
    expect(state.status).toBe('failed')
    expect(state.error).toEqual({
      code: 'MODEL_TIMEOUT',
      message: '模型响应超时',
      traceId: 'trace-1',
    })
  })

  it('ignores duplicate event ids', () => {
    let state = createInitialStreamState()

    state = reduceStreamEvent(state, { type: 'message_delta', eventId: 'evt-duplicate', delta: '只追加一次' })
    state = reduceStreamEvent(state, { type: 'message_delta', eventId: 'evt-duplicate', delta: '不应出现' })

    expect(state.content).toBe('只追加一次')
  })

  it('does not append tokens after done', () => {
    let state = createInitialStreamState()

    state = reduceStreamEvent(state, { type: 'done', eventId: 'evt-done', messageId: 'assistant-1' })
    state = reduceStreamEvent(state, { type: 'message_delta', eventId: 'evt-after-done', delta: 'late token' })

    expect(state.status).toBe('completed')
    expect(state.content).toBe('')
    expect(state.seenEventIds).toContain('evt-after-done')
  })
})
