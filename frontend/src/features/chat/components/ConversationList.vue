<template>
  <aside class="conversation-list">
    <div class="list-header">
      <h2>会话</h2>
      <el-button type="primary" size="small" :loading="creating" @click="$emit('create')" aria-label="新建会话">
        <el-icon><Plus /></el-icon>
      </el-button>
    </div>

    <div v-if="searchable" class="list-search">
      <el-input
        :model-value="keyword"
        clearable
        placeholder="搜索标题"
        @update:model-value="$emit('search', String($event))"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <div v-if="errorMessage" class="list-error">
      <el-alert
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
      />
      <TraceIdText v-if="errorTraceId" :trace-id="errorTraceId" />
    </div>

    <div v-else-if="loading" class="list-state muted-text">加载中...</div>
    <div v-else-if="conversations.length === 0" class="list-state muted-text">暂无会话</div>
    <button
      v-for="conversation in conversations"
      v-else
      :key="conversation.id"
      class="conversation-item"
      :class="{ active: conversation.id === activeConversationId }"
      type="button"
      @click="$emit('select', conversation.id)"
    >
      <span class="conversation-title">{{ conversation.title }}</span>
      <span class="conversation-preview">{{ conversation.lastMessagePreview || '还没有消息' }}</span>
      <span class="conversation-meta">
        {{ conversation.messageCount }} 条消息
        <template v-if="conversation.lastMessageAt"> · {{ formatTime(conversation.lastMessageAt) }}</template>
      </span>
    </button>
  </aside>
</template>

<script setup lang="ts">
import { Plus, Search } from '@element-plus/icons-vue'

import type { ConversationSummary } from '@/features/chat/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

defineProps<{
  conversations: ConversationSummary[]
  activeConversationId: string | null
  loading?: boolean
  creating?: boolean
  keyword?: string
  searchable?: boolean
  errorMessage?: string | null
  errorTraceId?: string | null
}>()

defineEmits<{
  create: []
  select: [conversationId: string]
  search: [keyword: string]
}>()

function formatTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
</script>

<style scoped>
.conversation-list {
  display: flex;
  min-height: 0;
  flex-direction: column;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  overflow-y: auto;
}

.list-header {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6eaf0;
  padding: 14px;
}

.list-search {
  flex: none;
  border-bottom: 1px solid #e6eaf0;
  padding: 12px 14px;
}

.list-error {
  display: grid;
  gap: 10px;
  padding: 12px 14px 0;
}

.list-header h2 {
  margin: 0;
  color: #182233;
  font-size: 16px;
}

.list-state {
  padding: 16px 14px;
}

.conversation-item {
  display: grid;
  gap: 6px;
  width: 100%;
  border: 0;
  border-bottom: 1px solid #edf1f5;
  background: #fff;
  padding: 14px;
  text-align: left;
  cursor: pointer;
}

.conversation-item:hover,
.conversation-item.active {
  background: #eef5ff;
}

.conversation-title {
  overflow: hidden;
  color: #1f2a3d;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-preview {
  overflow: hidden;
  color: #69778d;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-meta {
  color: #8a96a8;
  font-size: 12px;
}
</style>
