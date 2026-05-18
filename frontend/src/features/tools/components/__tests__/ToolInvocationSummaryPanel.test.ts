import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import ToolInvocationSummaryPanel from '@/features/tools/components/ToolInvocationSummaryPanel.vue'
import type { ToolInvocationSummary } from '@/features/tools/types'

describe('ToolInvocationSummaryPanel', () => {
  it('renders aggregate rates and top tool counts', async () => {
    render(ToolInvocationSummaryPanel, {
      props: {
        summary: {
          total: 8,
          running: 0,
          success: 5,
          failed: 2,
          denied: 1,
          timeout: 0,
          failureRate: 0.25,
          deniedRate: 0.125,
          timeoutRate: 0,
          averageDurationMs: 87.6,
          statusCounts: [],
          toolCounts: [
            {
              toolCode: 'log.search',
              toolName: '日志检索',
              toolType: 'LOG_SEARCH',
              count: 4,
              failed: 2,
              denied: 1,
              timeout: 0,
              averageDurationMs: 91.2,
            },
          ],
          invocations: {
            items: [],
            page: 0,
            size: 20,
            total: 8,
            totalPages: 1,
          },
        } satisfies ToolInvocationSummary,
      },
    })

    expect(await screen.findByText('总调用')).toBeInTheDocument()
    expect(screen.getByText('25.0%')).toBeInTheDocument()
    expect(screen.getByText('12.5%')).toBeInTheDocument()
    expect(await screen.findByText('log.search')).toBeInTheDocument()
    expect(screen.getAllByText('日志检索')).toHaveLength(2)
    expect(screen.getByText('91 ms')).toBeInTheDocument()
  })
})
