import { httpClient } from '@/shared/api/httpClient'
import type { ItemPageResponse } from '@/shared/types/api'
import type {
  ConversationCreateResponse,
  ConversationDetail,
  ConversationSummary,
  CancelMessageResponse,
  CreateConversationRequest,
  SendMessageRequest,
  SendMessageResponse,
} from '@/features/chat/types'

export const conversationsApi = {
  async createConversation(request: CreateConversationRequest = {}): Promise<ConversationCreateResponse> {
    const { data } = await httpClient.post<ConversationCreateResponse>('/conversations', request)
    return data
  },

  async queryConversations(params: {
    page: number
    size: number
    keyword?: string
  }): Promise<ItemPageResponse<ConversationSummary>> {
    const { data } = await httpClient.get<ItemPageResponse<ConversationSummary>>('/conversations', { params })
    return data
  },

  async getConversation(conversationId: string, params: { page: number; size: number }): Promise<ConversationDetail> {
    const { data } = await httpClient.get<ConversationDetail>(`/conversations/${conversationId}`, { params })
    return data
  },

  async sendMessage(conversationId: string, request: SendMessageRequest): Promise<SendMessageResponse> {
    const { data } = await httpClient.post<SendMessageResponse>(`/conversations/${conversationId}/messages`, request)
    return data
  },

  async cancelMessage(conversationId: string, messageId: string): Promise<CancelMessageResponse> {
    const { data } = await httpClient.post<CancelMessageResponse>(
      `/conversations/${conversationId}/messages/${messageId}/cancel`,
    )
    return data
  },
}
