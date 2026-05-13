import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import MarkdownContent from '@/shared/components/MarkdownContent.vue'

describe('MarkdownContent', () => {
  it('renders markdown formatting', () => {
    render(MarkdownContent, {
      props: {
        content: '**Problem**\n\n- First item\n- Second item',
      },
    })

    expect(screen.getByText('Problem').tagName).toBe('STRONG')
    expect(screen.getByText('First item')).toBeInTheDocument()
    expect(screen.getByText('Second item')).toBeInTheDocument()
  })

  it('sanitizes unsafe html', () => {
    const { container } = render(MarkdownContent, {
      props: {
        content: '<script>alert("xss")</script> **safe**',
      },
    })

    expect(container.querySelector('script')).not.toBeInTheDocument()
    expect(screen.getByText('safe')).toBeInTheDocument()
  })
})
