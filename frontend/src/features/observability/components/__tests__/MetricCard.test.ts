import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import MetricCard from '@/features/observability/components/MetricCard.vue'

describe('MetricCard', () => {
  it('renders label, value, hint and tag', () => {
    render(MetricCard, {
      props: {
        label: '模型调用耗时',
        value: '123 ms',
        hint: '10 次调用',
        tone: 'warning',
        tag: 'SLO',
      },
    })

    expect(screen.getByText('模型调用耗时')).toBeInTheDocument()
    expect(screen.getByText('123 ms')).toBeInTheDocument()
    expect(screen.getByText('10 次调用')).toBeInTheDocument()
    expect(screen.getByText('SLO')).toBeInTheDocument()
  })
})
