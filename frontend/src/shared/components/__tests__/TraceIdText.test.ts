import { render, screen } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import TraceIdText from '@/shared/components/TraceIdText.vue'

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
    },
  }
})

describe('TraceIdText', () => {
  beforeEach(() => {
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: {
        writeText: vi.fn().mockResolvedValue(undefined),
      },
    })
  })

  it('renders and copies trace id', async () => {
    render(TraceIdText, {
      props: {
        traceId: 'trace-frontend-001',
      },
    })

    await userEvent.click(screen.getByRole('button', { name: /traceId:/ }))

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith('trace-frontend-001')
  })
})
