import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import ToolDefinitionTable from '@/features/tools/components/ToolDefinitionTable.vue'
import type { ToolDefinition } from '@/features/tools/types'

describe('ToolDefinitionTable', () => {
  it('renders tool definitions and availability states', async () => {
    render(ToolDefinitionTable, {
      props: {
        tools: [
          toolDefinition({
            code: 'ticket.query',
            name: '工单查询',
            toolType: 'BUSINESS_QUERY',
            permissionCode: 'tool:ticket:query',
            riskLevel: 'LOW',
            enabled: true,
            available: true,
          }),
          toolDefinition({
            code: 'log.search',
            name: '日志检索',
            toolType: 'LOG_SEARCH',
            permissionCode: 'tool:log:search',
            riskLevel: 'MEDIUM',
            enabled: false,
            available: false,
          }),
        ],
        height: 360,
      },
    })

    expect(await screen.findByText('工单查询')).toBeInTheDocument()
    expect(screen.getByText('tool:ticket:query')).toBeInTheDocument()
    expect(screen.getByText('业务查询')).toBeInTheDocument()
    expect(screen.getByText('低风险')).toBeInTheDocument()
    expect(screen.getAllByText('日志检索')).toHaveLength(2)
    expect(screen.getByText('中风险')).toBeInTheDocument()
    expect(screen.getByText('停用')).toBeInTheDocument()
    expect(screen.getByText('不可用')).toBeInTheDocument()
  })
})

function toolDefinition(overrides: Partial<ToolDefinition>): ToolDefinition {
  return {
    id: crypto.randomUUID(),
    code: 'tool.code',
    name: '工具',
    toolType: 'BUSINESS_QUERY',
    description: '工具描述',
    permissionCode: 'tool:demo:query',
    riskLevel: 'LOW',
    enabled: true,
    available: true,
    inputSchema: '{}',
    outputSchema: '{}',
    createdAt: '2026-05-14T00:00:00Z',
    updatedAt: '2026-05-14T00:00:00Z',
    ...overrides,
  }
}
