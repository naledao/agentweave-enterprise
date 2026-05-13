<template>
  <section class="chat-workspace">
    <ConversationList
      :conversations="conversations"
      :active-conversation-id="activeConversationId"
      :loading="conversationsQuery.isFetching.value"
      :creating="createConversationMutation.isPending.value"
      :keyword="keyword"
      searchable
      :error-message="conversationListError?.message ?? null"
      :error-trace-id="conversationListError?.traceId ?? null"
      @create="createConversation"
      @select="selectConversation"
      @search="searchConversations"
    />

    <main class="conversation-panel">
      <div class="conversation-header">
        <div>
          <p class="eyebrow">Agent Console</p>
          <h1>{{ activeConversation?.title ?? '企业知识与任务编排' }}</h1>
        </div>
        <el-tag :type="streamStatusTag.type" effect="plain">{{ streamStatusTag.label }}</el-tag>
      </div>

      <div v-if="pageErrorView" class="page-error">
        <el-alert
          :title="pageErrorView.message"
          type="error"
          :closable="false"
          show-icon
        />
        <TraceIdText v-if="pageErrorView.traceId" :trace-id="pageErrorView.traceId" />
      </div>

      <ChatMessageList
        v-loading="conversationQuery.isFetching.value"
        :messages="visibleMessages"
        :stream="streamState"
        :page="messagePage"
        :size="messageSize"
        :total="messageTotal"
        @page-change="changeMessagePage"
      />

      <ChatInput
        :disabled="inputDisabled"
        :loading="sendMessageMutation.isPending.value || streamState.status === 'connecting'"
        :streaming="isStreaming"
        @send="sendMessage"
        @cancel="cancelStream"
      />
    </main>

    <aside class="context-panel">
      <AssistantGenerationPanel :messages="assistantPlaceholders" />
      <CitationPanel :citations="streamState.citations" />
      <GraphPathPanel :graph-paths="streamState.graphPaths" />
      <ToolCallPanel :invocations="streamState.toolInvocations" />
      <WorkflowStepPanel :steps="streamState.workflowSteps" />
    </aside>
  </section>
</template>

<script setup lang="ts">
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { openChatStream } from '@/features/chat/api/chatStreamClient'
import { conversationsApi } from '@/features/chat/api/conversationsApi'
import AssistantGenerationPanel from '@/features/chat/components/AssistantGenerationPanel.vue'
import ChatInput from '@/features/chat/components/ChatInput.vue'
import ChatMessageList from '@/features/chat/components/ChatMessageList.vue'
import CitationPanel from '@/features/chat/components/CitationPanel.vue'
import ConversationList from '@/features/chat/components/ConversationList.vue'
import GraphPathPanel from '@/features/chat/components/GraphPathPanel.vue'
import ToolCallPanel from '@/features/chat/components/ToolCallPanel.vue'
import WorkflowStepPanel from '@/features/chat/components/WorkflowStepPanel.vue'
import { createInitialStreamState, reduceStreamEvent } from '@/features/chat/composables/streamReducer'
import type { ChatMessage, ChatStreamEvent, ChatStreamState, ConversationDetail } from '@/features/chat/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'
import { getApiErrorDisplay } from '@/shared/utils/apiError'

interface StreamConnection {
  close: () => void
}

interface ActiveStreamTask {
  conversationId: string
  messageId: string
}

interface SendMessagePayload {
  conversationId: string
  content: string
}

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const pageError = ref<unknown | null>(null)
const keyword = ref('')
const messagePage = ref(0)
const messageSize = 20
const streamConnection = ref<StreamConnection | null>(null)
const activeStreamTask = ref<ActiveStreamTask | null>(null)
const activeAssistantMessageId = ref<string | null>(null)
const streamState = reactive<ChatStreamState>(createInitialStreamState())
const activeConversationId = computed(() => {
  const conversationId = route.query.conversationId
  return typeof conversationId === 'string' && conversationId.trim() ? conversationId : null
})

const conversationsQuery = useQuery({
  queryKey: computed(() => ['conversations', { page: 0, size: 20, keyword: keyword.value || undefined }]),
  queryFn: () => conversationsApi.queryConversations({ page: 0, size: 20, keyword: keyword.value || undefined }),
})

const conversationQuery = useQuery({
  queryKey: computed(() => ['conversation', activeConversationId.value, { page: messagePage.value, size: messageSize }]),
  queryFn: () => {
    if (!activeConversationId.value) {
      throw new Error('missing conversation')
    }

    return conversationsApi.getConversation(activeConversationId.value, { page: messagePage.value, size: messageSize })
  },
  enabled: computed(() => Boolean(activeConversationId.value)),
})

const createConversationMutation = useMutation({
  mutationFn: () => conversationsApi.createConversation({ title: '新的对话' }),
  onSuccess: async (conversation) => {
    pageError.value = null
    keyword.value = ''
    messagePage.value = 0
    cancelStream()
    assignStreamState(createInitialStreamState())
    await queryClient.invalidateQueries({ queryKey: ['conversations'] })
    await router.push({ name: 'Chat', query: { conversationId: conversation.id } })
  },
  onError: (error) => {
    pageError.value = error
  },
})

const sendMessageMutation = useMutation({
  mutationFn: async ({ conversationId, content }: SendMessagePayload) => {
    return conversationsApi.sendMessage(conversationId, { content, responseMode: 'STREAM' })
  },
  onSuccess: async (response) => {
    pageError.value = null
    activeAssistantMessageId.value = response.assistantMessageId
    activeStreamTask.value = {
      conversationId: response.conversationId,
      messageId: response.assistantMessageId,
    }
    startStream(response.conversationId)
    await queryClient.invalidateQueries({ queryKey: ['conversations'] })
    await queryClient.invalidateQueries({ queryKey: ['conversation', response.conversationId] })
  },
  onError: (error) => {
    pageError.value = error
    assignStreamState(createInitialStreamState())
  },
})

const cancelMessageMutation = useMutation({
  mutationFn: async ({ conversationId, messageId }: { conversationId: string; messageId: string }) => {
    return conversationsApi.cancelMessage(conversationId, messageId)
  },
  onSuccess: async (response) => {
    pageError.value = null
    assignStreamState({ ...streamState, status: 'cancelled' })
    activeAssistantMessageId.value = null
    activeStreamTask.value = null
    await queryClient.invalidateQueries({ queryKey: ['conversation', response.conversationId] })
    await queryClient.invalidateQueries({ queryKey: ['conversations'] })
  },
  onError: (error) => {
    pageError.value = error
  },
})

const conversations = computed(() => conversationsQuery.data.value?.items ?? [])
const activeConversation = computed(() => conversationQuery.data.value ?? null)
const conversationListError = computed(() =>
  conversationsQuery.error.value ? getApiErrorDisplay(conversationsQuery.error.value, '会话加载失败') : null,
)
const pageErrorView = computed(() =>
  pageError.value ? getApiErrorDisplay(pageError.value, '操作失败') : null,
)
const messages = computed<ChatMessage[]>(() => activeConversation.value?.messages ?? [])
const assistantPlaceholders = computed<ChatMessage[]>(() =>
  messages.value.filter((message) => isAssistantPlaceholder(message)),
)
const visibleMessages = computed<ChatMessage[]>(() =>
  messages.value.filter((message) => !isAssistantPlaceholder(message)),
)
const messageTotal = computed(() => activeConversation.value?.messageTotal ?? 0)
const isStreaming = computed(() => ['connecting', 'streaming', 'tool_calling'].includes(streamState.status))
const inputDisabled = computed(() => sendMessageMutation.isPending.value || isStreaming.value)
const streamStatusTag = computed(() => {
  const map: Record<ChatStreamState['status'], { label: string; type: 'info' | 'success' | 'warning' | 'danger' }> = {
    idle: { label: '就绪', type: 'info' },
    connecting: { label: '连接中', type: 'warning' },
    streaming: { label: '生成中', type: 'warning' },
    tool_calling: { label: '调用工具', type: 'warning' },
    completed: { label: '完成', type: 'success' },
    failed: { label: '失败', type: 'danger' },
    cancelled: { label: '已停止', type: 'info' },
  }

  return map[streamState.status]
})

onBeforeUnmount(() => {
  stopStream({ notifyServer: true, detached: true })
})

watch(activeConversationId, (nextConversationId, previousConversationId) => {
  if (!previousConversationId || nextConversationId === previousConversationId) {
    return
  }

  messagePage.value = 0
  cancelStream()
  assignStreamState(createInitialStreamState())
})

function createConversation(): void {
  createConversationMutation.mutate()
}

async function selectConversation(conversationId: string): Promise<void> {
  pageError.value = null
  messagePage.value = 0
  cancelStream()
  assignStreamState(createInitialStreamState())
  await router.push({ name: 'Chat', query: { conversationId } })
}

function searchConversations(value: string): void {
  keyword.value = value
}

function changeMessagePage(page: number): void {
  pageError.value = null
  cancelStream()
  messagePage.value = page
}

async function sendMessage(content: string): Promise<void> {
  pageError.value = null
  const conversationId = activeConversationId.value ?? await ensureConversation()
  appendOptimisticUserMessage(conversationId, content)
  assignStreamState({ ...createInitialStreamState(), status: 'connecting' })
  sendMessageMutation.mutate({ conversationId, content })
}

function cancelStream(): void {
  stopStream({ notifyServer: true })
}

function stopStream(options: { notifyServer: boolean; detached?: boolean }): void {
  const streamTask = activeStreamTask.value
  const conversationId = streamTask?.conversationId ?? activeConversationId.value
  const messageId = streamTask?.messageId ?? activeAssistantMessageId.value ?? streamState.assistantMessageId
  streamConnection.value?.close()
  streamConnection.value = null
  if (isStreaming.value) {
    assignStreamState({ ...streamState, status: 'cancelled' })
  }
  if (options.notifyServer && conversationId && messageId && !cancelMessageMutation.isPending.value) {
    if (options.detached) {
      activeStreamTask.value = null
      activeAssistantMessageId.value = null
      void conversationsApi.cancelMessage(conversationId, messageId)
      return
    }

    cancelMessageMutation.mutate({ conversationId, messageId })
  }
}

async function ensureConversation(): Promise<string> {
  if (activeConversationId.value) {
    return activeConversationId.value
  }

  const conversation = await conversationsApi.createConversation({ title: '新的对话' })
  await queryClient.invalidateQueries({ queryKey: ['conversations'] })
  messagePage.value = 0
  await router.push({ name: 'Chat', query: { conversationId: conversation.id } })
  return conversation.id
}

function startStream(conversationId: string): void {
  streamConnection.value?.close()
  assignStreamState({ ...createInitialStreamState(), status: 'connecting' })
  streamConnection.value = openChatStream(conversationId, {
    onEvent: handleStreamEvent,
    onError: handleStreamEvent,
  })
}

function handleStreamEvent(event: ChatStreamEvent): void {
  assignStreamState(reduceStreamEvent(streamState, event))

  if (event.type === 'done' || event.type === 'error') {
    streamConnection.value?.close()
    streamConnection.value = null
    activeAssistantMessageId.value = null
    activeStreamTask.value = null
  }

  if (event.type === 'done') {
    void queryClient.invalidateQueries({ queryKey: ['conversation', activeConversationId.value] })
  }

  if (event.type === 'error') {
    void queryClient.invalidateQueries({ queryKey: ['conversation', activeConversationId.value] })
  }
}

function appendOptimisticUserMessage(conversationId: string, content: string): void {
  const now = new Date().toISOString()
  queryClient.setQueryData<ConversationDetail>(
    ['conversation', conversationId, { page: messagePage.value, size: messageSize }],
    (existing) => {
      const baseConversation = existing ?? {
        id: conversationId,
        title: '新的对话',
        status: 'ACTIVE',
        messageCount: 0,
        createdAt: now,
        updatedAt: now,
        messages: [],
        messagePage: messagePage.value,
        messageSize,
        messageTotal: 0,
        messageTotalPages: 0,
        traceId: '',
      }

      return {
        ...baseConversation,
        messageCount: baseConversation.messageCount + 2,
        messageTotal: baseConversation.messageTotal + 2,
        messages: [
          ...baseConversation.messages,
          {
            id: `local-${Date.now()}`,
            conversationId,
            role: 'USER',
            content,
            status: 'SUCCEEDED',
            errorCode: null,
            errorMessage: null,
            metadata: '{}',
            traceId: null,
            citations: [],
            graphPaths: [],
            toolCalls: [],
            createdAt: now,
          },
          {
            id: `local-assistant-${Date.now()}`,
            conversationId,
            role: 'ASSISTANT',
            content: '',
            status: 'PENDING',
            errorCode: null,
            errorMessage: null,
            metadata: '{}',
            traceId: null,
            citations: [],
            graphPaths: [],
            toolCalls: [],
            createdAt: now,
          },
        ],
      }
    },
  )
}

function isAssistantPlaceholder(message: ChatMessage): boolean {
  return (
    message.role === 'ASSISTANT' &&
    ['PENDING', 'STREAMING'].includes(message.status) &&
    !message.content.trim()
  )
}

function assignStreamState(next: ChatStreamState): void {
  streamState.status = next.status
  streamState.assistantMessageId = next.assistantMessageId
  streamState.content = next.content
  streamState.seenEventIds = next.seenEventIds
  streamState.citations = next.citations
  streamState.graphPaths = next.graphPaths
  streamState.toolInvocations = next.toolInvocations
  streamState.workflowSteps = next.workflowSteps
  streamState.error = next.error
}
</script>

<style scoped>
.chat-workspace {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 320px;
  gap: 16px;
  width: 100%;
  height: 100%;
  min-height: 0;
}

.conversation-panel,
.context-panel {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.conversation-header {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6eaf0;
  padding: 18px 20px;
}

.eyebrow {
  margin: 0 0 4px;
  color: #5b6b84;
  font-size: 13px;
}

h1 {
  margin: 0;
  color: #182233;
  font-size: 20px;
  font-weight: 700;
}

.page-error {
  flex: none;
  margin: 14px 16px 0;
  display: grid;
  gap: 10px;
}

.context-panel {
  gap: 18px;
  overflow-y: auto;
  padding: 16px;
  background: #f8fafc;
}

@media (max-width: 1180px) {
  .chat-workspace {
    grid-template-columns: 220px minmax(0, 1fr);
    overflow-y: auto;
  }

  .context-panel {
    grid-column: 1 / -1;
    min-height: 320px;
  }
}

@media (max-width: 820px) {
  .chat-workspace {
    grid-template-columns: 1fr;
  }

  .conversation-panel,
  .conversation-list {
    min-height: 520px;
  }
}
</style>
