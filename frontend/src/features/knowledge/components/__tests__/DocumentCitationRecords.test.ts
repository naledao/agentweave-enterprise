import { render, screen } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'

import DocumentCitationRecords from '@/features/knowledge/components/DocumentCitationRecords.vue'

describe('DocumentCitationRecords', () => {
  it('shows citation records and navigates to the conversation', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/app/chat',
          name: 'Chat',
          component: { template: '<div />' },
        },
      ],
    })
    await router.push('/app/chat')
    await router.isReady()

    render(DocumentCitationRecords, {
      global: {
        plugins: [router],
      },
      props: {
        records: [
          {
            conversationId: 'conversation-1',
            messageId: 'message-1',
            messagePreview: 'Use the order runbook citation.',
            traceId: 'trace-1',
            createdAt: '2026-05-13T08:00:00Z',
          },
        ],
      },
    })

    expect(screen.getByText('Use the order runbook citation.')).toBeInTheDocument()
    expect(screen.getByText('trace-1')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: '查看会话' }))

    expect(router.currentRoute.value.query.conversationId).toBe('conversation-1')
  })
})
