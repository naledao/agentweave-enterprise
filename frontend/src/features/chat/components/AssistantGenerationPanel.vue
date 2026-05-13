<template>
  <section class="panel-section">
    <h3>生成状态</h3>
    <div v-if="messages.length === 0" class="panel-empty">暂无生成任务</div>
    <article
      v-for="message in messages"
      v-else
      :key="message.id"
      class="generation-card"
    >
      <div class="generation-title">
        <strong>AgentWeave</strong>
        <el-tag :type="statusTag(message.status).type" effect="plain">
          {{ statusTag(message.status).label }}
        </el-tag>
      </div>
      <p>{{ message.content || statusHint(message.status) }}</p>
      <TraceIdText v-if="message.traceId" :trace-id="message.traceId" />
    </article>
  </section>
</template>

<script setup lang="ts">
import type { ChatMessage, ChatMessageStatus } from '@/features/chat/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

defineProps<{
  messages: ChatMessage[]
}>()

function statusTag(status: ChatMessageStatus): { label: string; type: 'info' | 'success' | 'warning' | 'danger' } {
  const map: Record<ChatMessageStatus, { label: string; type: 'info' | 'success' | 'warning' | 'danger' }> = {
    PENDING: { label: '等待生成', type: 'warning' },
    STREAMING: { label: '生成中', type: 'warning' },
    SUCCEEDED: { label: '完成', type: 'success' },
    FAILED: { label: '失败', type: 'danger' },
    CANCELLED: { label: '已停止', type: 'info' },
  }

  return map[status]
}

function statusHint(status: ChatMessageStatus): string {
  const map: Record<ChatMessageStatus, string> = {
    PENDING: '助手消息已创建，等待模型开始输出。',
    STREAMING: '模型正在生成回复，正文在聊天区流式展示。',
    SUCCEEDED: '助手回复已写入会话记录。',
    FAILED: '助手回复生成失败，请查看错误信息。',
    CANCELLED: '本次生成已停止。',
  }

  return map[status]
}
</script>

<style scoped>
.panel-section {
  display: grid;
  gap: 10px;
}

.panel-section h3 {
  margin: 0;
  color: #182233;
  font-size: 15px;
}

.panel-empty {
  color: #69778d;
  font-size: 13px;
}

.generation-card {
  display: grid;
  gap: 8px;
  border: 1px solid #dfe5ee;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.generation-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.generation-card p {
  margin: 0;
  color: #39485f;
  font-size: 13px;
  line-height: 1.6;
}
</style>
