<template>
  <el-table
    v-loading="loading"
    class="rag-retrieval-table"
    :data="logs"
    height="100%"
    row-key="id"
    empty-text="暂无 RAG 检索日志"
  >
    <el-table-column label="状态" width="105">
      <template #default="{ row }: { row: RagRetrievalLog }">
        <el-tag :type="statusType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="模式" width="115">
      <template #default="{ row }: { row: RagRetrievalLog }">{{ row.retrievalMode }}</template>
    </el-table-column>
    <el-table-column label="问题" min-width="220">
      <template #default="{ row }: { row: RagRetrievalLog }">
        <span class="summary-text">{{ row.query }}</span>
      </template>
    </el-table-column>
    <el-table-column label="业务域" width="120">
      <template #default="{ row }: { row: RagRetrievalLog }">{{ row.businessDomain || '-' }}</template>
    </el-table-column>
    <el-table-column label="引用" width="85" align="right">
      <template #default="{ row }: { row: RagRetrievalLog }">{{ row.citationCount }}</template>
    </el-table-column>
    <el-table-column label="Chunk" width="90" align="right">
      <template #default="{ row }: { row: RagRetrievalLog }">{{ row.matchedChunkIds.length }}</template>
    </el-table-column>
    <el-table-column label="耗时" width="100" align="right">
      <template #default="{ row }: { row: RagRetrievalLog }">{{ formatDuration(row.durationMs) }}</template>
    </el-table-column>
    <el-table-column label="创建时间" min-width="170">
      <template #default="{ row }: { row: RagRetrievalLog }">{{ formatDateTime(row.createdAt) }}</template>
    </el-table-column>
    <el-table-column label="错误摘要" min-width="220">
      <template #default="{ row }: { row: RagRetrievalLog }">
        <span class="summary-text">{{ row.errorMessage || row.scoreSummary || '-' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="traceId" min-width="230">
      <template #default="{ row }: { row: RagRetrievalLog }">
        <TraceIdText v-if="row.traceId" :trace-id="row.traceId" />
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import type { RagRetrievalLog, RagRetrievalStatus } from '@/features/observability/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

defineProps<{
  logs: RagRetrievalLog[]
  loading?: boolean
}>()

function statusType(status: RagRetrievalStatus): 'success' | 'warning' | 'danger' | 'info' {
  const normalized = status.toUpperCase()
  if (normalized === 'SUCCESS') {
    return 'success'
  }
  if (normalized === 'FAILED') {
    return 'danger'
  }
  if (normalized === 'DEGRADED') {
    return 'warning'
  }
  return 'info'
}

function statusLabel(status: RagRetrievalStatus): string {
  const map: Record<string, string> = {
    PROCESSING: '处理中',
    SUCCESS: '成功',
    FAILED: '失败',
    DEGRADED: '降级',
  }
  return map[status.toUpperCase()] ?? status
}

function formatDateTime(value: string | null): string {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function formatDuration(value: number | null): string {
  if (value === null || value === undefined) {
    return '-'
  }

  return `${Math.round(value)} ms`
}
</script>

<style scoped>
.rag-retrieval-table {
  width: 100%;
}

.summary-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #69778d;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
