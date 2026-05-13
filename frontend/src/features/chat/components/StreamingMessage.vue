<template>
  <article v-if="visible" class="streaming-message">
    <div class="message-meta">
      <strong>AgentWeave</strong>
      <el-tag :type="tag.type" effect="plain">{{ tag.label }}</el-tag>
    </div>
    <MarkdownContent v-if="content" :content="content" />
    <p v-else class="muted-text">正在连接模型流...</p>
    <el-alert
      v-if="error"
      class="stream-error"
      :title="error.message"
      type="error"
      :closable="false"
      show-icon
    />
    <TraceIdText v-if="error?.traceId" class="stream-trace" :trace-id="error.traceId" />
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { ChatStreamState } from '@/features/chat/types'
import MarkdownContent from '@/shared/components/MarkdownContent.vue'
import TraceIdText from '@/shared/components/TraceIdText.vue'

const props = defineProps<{
  stream: ChatStreamState
}>()

const visible = computed(() => props.stream.status !== 'idle' || Boolean(props.stream.content))
const content = computed(() => props.stream.content)
const error = computed(() => props.stream.error)
const tag = computed(() => {
  const map: Record<ChatStreamState['status'], { label: string; type: 'info' | 'success' | 'warning' | 'danger' }> = {
    idle: { label: '空闲', type: 'info' },
    connecting: { label: '连接中', type: 'warning' },
    streaming: { label: '生成中', type: 'warning' },
    tool_calling: { label: '调用工具', type: 'warning' },
    completed: { label: '完成', type: 'success' },
    failed: { label: '失败', type: 'danger' },
    cancelled: { label: '已停止', type: 'info' },
  }

  return map[props.stream.status]
})
</script>

<style scoped>
.streaming-message {
  width: min(100%, 1120px);
  align-self: flex-start;
  border: 1px solid #dfe5ee;
  border-radius: 8px;
  background: #fff;
  padding: 14px 16px;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.stream-error {
  margin-top: 12px;
}

.stream-trace {
  margin-top: 10px;
}
</style>
