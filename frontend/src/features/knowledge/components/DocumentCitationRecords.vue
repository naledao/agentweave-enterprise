<template>
  <div class="citation-records">
    <div class="record-header">
      <span>回答摘要</span>
      <span>引用时间</span>
      <span>Trace</span>
      <span>操作</span>
    </div>

    <div v-if="records.length === 0" class="empty-records">暂无引用记录</div>

    <article
      v-for="record in records"
      v-else
      :key="record.messageId"
      class="record-row"
    >
      <p class="message-preview">{{ record.messagePreview || '-' }}</p>
      <span class="record-time">{{ formatDateTime(record.createdAt) }}</span>
      <TraceIdText v-if="record.traceId" :trace-id="record.traceId" />
      <span v-else class="muted-text">-</span>
      <el-button text type="primary" @click="openConversation(record)">
        查看会话
      </el-button>
    </article>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

import type { DocumentCitationRecord } from '@/features/knowledge/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

defineProps<{
  records: DocumentCitationRecord[]
}>()

const router = useRouter()

async function openConversation(record: DocumentCitationRecord): Promise<void> {
  await router.push({ name: 'Chat', query: { conversationId: record.conversationId } })
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
</script>

<style scoped>
.citation-records {
  display: grid;
  overflow: hidden;
  border: 1px solid #e6eaf0;
  border-radius: 8px;
}

.record-header,
.record-row {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 170px minmax(160px, 220px) 88px;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
}

.record-header {
  background: #f8fafc;
  color: #69778d;
  font-size: 12px;
  font-weight: 600;
}

.record-row {
  border-top: 1px solid #edf1f5;
}

.message-preview {
  display: -webkit-box;
  max-height: 44px;
  overflow: hidden;
  margin: 0;
  color: #39485f;
  font-size: 13px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.record-time {
  color: #39485f;
  font-size: 13px;
}

.empty-records {
  border-top: 1px solid #edf1f5;
  padding: 18px 14px;
  color: #69778d;
  font-size: 13px;
}

@media (max-width: 860px) {
  .record-header {
    display: none;
  }

  .record-row {
    grid-template-columns: 1fr;
    align-items: start;
  }
}
</style>
