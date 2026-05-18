import { defineStore } from 'pinia'

import { createInitialStreamState } from '@/features/chat/composables/streamReducer'
import type { ChatStreamState } from '@/features/chat/types'

interface ChatState {
  activeConversationId: string | null
  draftByConversationId: Record<string, string>
  stream: ChatStreamState
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    activeConversationId: null,
    draftByConversationId: {},
    stream: createInitialStreamState(),
  }),

  getters: {
    activeDraft: (state): string => {
      if (!state.activeConversationId) {
        return ''
      }

      return state.draftByConversationId[state.activeConversationId] ?? ''
    },
    isStreaming: (state): boolean =>
      ['connecting', 'streaming', 'tool_calling'].includes(state.stream.status),
  },

  actions: {
    setActiveConversation(conversationId: string | null): void {
      this.activeConversationId = conversationId
    },

    setDraft(conversationId: string, draft: string): void {
      this.draftByConversationId = {
        ...this.draftByConversationId,
        [conversationId]: draft,
      }
    },

    clearDraft(conversationId: string): void {
      const nextDrafts = { ...this.draftByConversationId }
      delete nextDrafts[conversationId]
      this.draftByConversationId = nextDrafts
    },

    setStreamState(stream: ChatStreamState): void {
      this.stream = stream
    },

    resetStream(): void {
      this.stream = createInitialStreamState()
    },
  },
})
