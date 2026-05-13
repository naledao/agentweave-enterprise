<template>
  <div ref="messageListRef" class="message-list">
    <div v-if="total > size" class="message-pagination">
      <el-pagination
        background
        small
        layout="prev, pager, next"
        :current-page="page + 1"
        :page-size="size"
        :total="total"
        @current-change="handlePageChange"
      />
    </div>

    <div v-if="messages.length === 0 && stream.status === 'idle'" class="empty-chat">
      <strong>开始一次企业知识问答</strong>
      <span>可以询问知识库内容、排障线索、工具查询或任务执行计划。</span>
    </div>
    <ChatMessageItem
      v-for="message in messages"
      :key="message.id"
      :message="message"
    />
    <StreamingMessage v-if="showStreamingMessage" :stream="stream" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'

import ChatMessageItem from '@/features/chat/components/ChatMessageItem.vue'
import StreamingMessage from '@/features/chat/components/StreamingMessage.vue'
import type { ChatMessage, ChatStreamState } from '@/features/chat/types'

const props = defineProps<{
  messages: ChatMessage[]
  stream: ChatStreamState
  page: number
  size: number
  total: number
}>()

const emit = defineEmits<{
  'page-change': [page: number]
}>()

const messageListRef = ref<HTMLElement | null>(null)
const showStreamingMessage = computed(() => {
  const hasStreamContent = props.stream.status !== 'idle' || Boolean(props.stream.content)
  if (!hasStreamContent) {
    return false
  }

  return !props.messages.some((message) => message.id === props.stream.assistantMessageId)
})

watch(
  () => [
    props.messages.length,
    props.messages.at(-1)?.id,
    props.stream.content.length,
    props.stream.status,
  ],
  () => {
    void scrollToBottom()
  },
  { flush: 'post' },
)

function handlePageChange(page: number): void {
  emit('page-change', page - 1)
}

async function scrollToBottom(): Promise<void> {
  await nextTick()
  const messageList = messageListRef.value
  if (!messageList) {
    return
  }

  messageList.scrollTop = messageList.scrollHeight
}
</script>

<style scoped>
.message-list {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  overflow-y: auto;
  padding: 20px;
}

.message-pagination {
  display: flex;
  justify-content: center;
}

.empty-chat {
  display: grid;
  place-items: center;
  align-content: center;
  min-height: 320px;
  gap: 8px;
  color: #69778d;
  text-align: center;
}

.empty-chat strong {
  color: #182233;
  font-size: 18px;
}
</style>
