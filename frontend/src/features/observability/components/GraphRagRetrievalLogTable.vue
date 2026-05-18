<template>
  <el-table
    v-loading="loading"
    class="graph-rag-table"
    :data="logs"
    height="100%"
    row-key="id"
    empty-text="暂无 GraphRAG 检索日志"
  >
    <el-table-column label="状态" width="110">
      <template #default="{ row }: { row: GraphRagRetrievalLog }">
        <GraphRagStatusTag :status="row.status" />
      </template>
    </el-table-column>
    <el-table-column label="模式" width="120">
      <template #default="{ row }: { row: GraphRagRetrievalLog }">
        <span class="mono short">{{ row.retrievalMode }}</span>
      </template>
    </el-table-column>
    <el-table-column label="问题摘要" min-width="240">
      <template #default="{ row }: { row: GraphRagRetrievalLog }">
        <span class="summary-text">{{ row.query }}</span>
      </template>
    </el-table-column>
    <el-table-column label="业务域" width="120">
      <template #default="{ row }: { row: GraphRagRetrievalLog }">{{ row.businessDomain || '-' }}</template>
    </el-table-column>
    <el-table-column label="匹配路径" width="100" align="right">
      <template #default="{ row }: { row: GraphRagRetrievalLog }">{{ row.matchedPathCount }}</template>
    </el-table-column>
    <el-table-column label="过滤后" width="100" align="right">
      <template #default="{ row }: { row: GraphRagRetrievalLog }">{{ row.filteredPathCount }}</template>
    </el-table-column>
    <el-table-column label="来源 Chunk" width="110" align="right">
      <template #default="{ row }: { row: GraphRagRetrievalLog }">{{ row.sourceChunkIds.length }}</template>
    </el-table-column>
    <el-table-column label="耗时" width="100" align="right">
      <template #default="{ row }: { row: GraphRagRetrievalLog }">{{ formatDuration(row.durationMs) }}</template>
    </el-table-column>
    <el-table-column label="创建时间" min-width="170">
      <template #default="{ row }: { row: GraphRagRetrievalLog }">{{ formatDateTime(row.createdAt) }}</template>
    </el-table-column>
    <el-table-column label="实体摘要" min-width="180">
      <template #default="{ row }: { row: GraphRagRetrievalLog }">
        <span class="summary-text">{{ row.resolvedEntities.join(', ') || '-' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="错误摘要" min-width="220">
      <template #default="{ row }: { row: GraphRagRetrievalLog }">
        <span class="summary-text">{{ row.errorMessage || '-' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="traceId" min-width="230">
      <template #default="{ row }: { row: GraphRagRetrievalLog }">
        <TraceIdText v-if="row.traceId" :trace-id="row.traceId" />
      </template>
    </el-table-column>
    <el-table-column label="workflowRunId" min-width="230">
      <template #default="{ row }: { row: GraphRagRetrievalLog }">
        <span class="mono">{{ row.workflowRunId || '-' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="workflowStepId" min-width="230">
      <template #default="{ row }: { row: GraphRagRetrievalLog }">
        <span class="mono">{{ row.workflowStepId || '-' }}</span>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import GraphRagStatusTag from '@/features/observability/components/GraphRagStatusTag.vue'
import type { GraphRagRetrievalLog } from '@/features/observability/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

defineProps<{
  logs: GraphRagRetrievalLog[]
  loading?: boolean
}>()

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

  return `${value} ms`
}
</script>

<style scoped>
.graph-rag-table {
  width: 100%;
}

.graph-rag-table :deep(.el-table__inner-wrapper) {
  height: 100%;
}

.mono {
  display: inline-block;
  max-width: 190px;
  overflow: hidden;
  color: #48566b;
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mono.short {
  max-width: 100px;
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
