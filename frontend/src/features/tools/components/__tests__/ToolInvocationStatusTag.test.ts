import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import ToolInvocationStatusTag from '@/features/tools/components/ToolInvocationStatusTag.vue'

describe('ToolInvocationStatusTag', () => {
  it.each([
    ['running', '执行中'],
    ['success', '成功'],
    ['failed', '失败'],
    ['denied', '拒绝'],
    ['timeout', '超时'],
  ] as const)('renders %s as %s', (status, label) => {
    render(ToolInvocationStatusTag, {
      props: { status },
    })

    expect(screen.getByText(label)).toBeInTheDocument()
  })
})
