import { afterEach, describe, expect, it, vi } from 'vitest'

import { conversationsApi } from '@/features/chat/api/conversationsApi'
import { httpClient } from '@/shared/api/httpClient'

describe('conversationsApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('calls cancel message endpoint', async () => {
    const post = vi.spyOn(httpClient, 'post').mockResolvedValue({
      data: {
        conversationId: 'conversation-1',
        messageId: 'assistant-1',
        status: 'CANCELLED',
        traceId: 'trace-1',
      },
    } as never)

    const response = await conversationsApi.cancelMessage('conversation-1', 'assistant-1')

    expect(post).toHaveBeenCalledWith('/conversations/conversation-1/messages/assistant-1/cancel')
    expect(response.status).toBe('CANCELLED')
  })
})
