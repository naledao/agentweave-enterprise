<template>
  <article class="message" :class="message.role.toLowerCase()">
    <div class="message-meta">
      <strong>{{ roleLabel }}</strong>
      <div class="message-meta-right">
        <el-tag
          v-if="message.status !== 'SUCCEEDED'"
          :type="statusTag.type"
          size="small"
          effect="plain"
        >
          {{ statusTag.label }}
        </el-tag>
        <span>{{ formattedTime }}</span>
      </div>
    </div>
    <MarkdownContent :content="message.content" />
    <RagTracePanel
      v-if="message.role === 'ASSISTANT'"
      :citations="message.citations"
      :graph-paths="message.graphPaths"
      :retrieval-mode="message.retrievalMode"
      :successful="message.status === 'SUCCEEDED'"
      @open-citations="$emit('open-context', 'citations')"
      @open-graph-paths="$emit('open-context', 'graphPaths')"
    />
    <TraceIdText v-if="message.traceId" class="message-trace" :trace-id="message.traceId" />
    <el-alert
      v-if="message.errorMessage"
      class="message-error"
      type="error"
      :title="message.errorMessage"
      :description="message.errorCode ?? undefined"
      :closable="false"
      show-icon
    />
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import RagTracePanel from '@/features/chat/components/RagTracePanel.vue'
import type { ChatMessage } from '@/features/chat/types'
import MarkdownContent from '@/shared/components/MarkdownContent.vue'
import TraceIdText from '@/shared/components/TraceIdText.vue'

const props = defineProps<{
  message: ChatMessage
}>()

defineEmits<{
  'open-context': [target: 'citations' | 'graphPaths']
}>()

const roleLabel = computed(() => {
  const labels: Record<ChatMessage['role'], string> = {
    USER: '你',
    ASSISTANT: 'AgentWeave',
    SYSTEM: '系统',
    TOOL: '工具',
  }

  return labels[props.message.role]
})

const formattedTime = computed(() => new Date(props.message.createdAt).toLocaleString())

const statusTag = computed(() => {
  const labels: Record<ChatMessage['status'], { label: string; type: 'info' | 'warning' | 'danger' }> = {
    PENDING: { label: '等待生成', type: 'warning' },
    SUCCEEDED: { label: '完成', type: 'info' },
    STREAMING: { label: '生成中', type: 'warning' },
    FAILED: { label: '失败', type: 'danger' },
    CANCELLED: { label: '已停止', type: 'info' },
  }

  return labels[props.message.status]
})
</script>

<style scoped>
.message {
  width: min(100%, 1120px);
  border: 1px solid #dfe5ee;
  border-radius: 8px;
  background: #fff;
  padding: 14px 16px;
}

.message.user {
  width: fit-content;
  max-width: min(72%, 560px);
  align-self: flex-end;
  border-color: #a9c5ff;
  background: #f1f6ff;
}

.message.assistant {
  align-self: flex-start;
}

.message.system {
  width: fit-content;
  max-width: min(72%, 640px);
  align-self: center;
  background: #f7f9fc;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #69778d;
  font-size: 13px;
}

.message-meta strong {
  color: #182233;
}

.message-meta-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.message-error {
  margin-top: 10px;
}

.message-trace {
  margin-top: 10px;
}
</style>
