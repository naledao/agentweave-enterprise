import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import ToolInvocationCard from '@/features/tools/components/ToolInvocationCard.vue'

describe('ToolInvocationCard', () => {
  it('renders invocation summaries and trace id', () => {
    render(ToolInvocationCard, {
      props: {
        invocation: {
          toolCallId: 'call-1',
          toolName: '日志检索',
          status: 'SUCCEEDED',
          inputSummary: '查询 error 关键字',
          resultSummary: '命中 3 条日志',
          latencyMs: 120,
          traceId: 'trace-tool-1',
        },
      },
    })

    expect(screen.getByText('日志检索')).toBeInTheDocument()
    expect(screen.getByText('成功')).toBeInTheDocument()
    expect(screen.getByText('查询 error 关键字')).toBeInTheDocument()
    expect(screen.getByText('命中 3 条日志')).toBeInTheDocument()
    expect(screen.getByText('120 ms')).toBeInTheDocument()
    expect(screen.getByText('trace-tool-1')).toBeInTheDocument()
  })

  it('renders denied status and error message', () => {
    render(ToolInvocationCard, {
      props: {
        invocation: {
          id: 'invocation-1',
          toolCode: 'database.readonly',
          toolName: '数据库只读查询',
          status: 'denied',
          errorMessage: '缺少工具权限',
        },
      },
    })

    expect(screen.getByText('数据库只读查询')).toBeInTheDocument()
    expect(screen.getByText('database.readonly')).toBeInTheDocument()
    expect(screen.getByText('拒绝')).toBeInTheDocument()
    expect(screen.getByText('缺少工具权限')).toBeInTheDocument()
  })
})
