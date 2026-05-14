import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import ToolRiskTag from '@/features/tools/components/ToolRiskTag.vue'

describe('ToolRiskTag', () => {
  it.each([
    ['LOW', '低风险'],
    ['MEDIUM', '中风险'],
    ['HIGH', '高风险'],
  ] as const)('renders %s as %s', (riskLevel, label) => {
    render(ToolRiskTag, {
      props: { riskLevel },
    })

    expect(screen.getByText(label)).toBeInTheDocument()
  })
})
