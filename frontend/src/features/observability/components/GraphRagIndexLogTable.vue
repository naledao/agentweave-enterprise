<template>
  <el-table
    v-loading="loading"
    class="graph-rag-table"
    :data="logs"
    height="100%"
    row-key="id"
    empty-text="暂无 GraphRAG 构建日志"
  >
    <el-table-column label="状态" width="110">
      <template #default="{ row }: { row: GraphRagIndexLog }">
        <GraphRagStatusTag :status="row.status" />
      </template>
    </el-table-column>
    <el-table-column label="文档" min-width="210">
      <template #default="{ row }: { row: GraphRagIndexLog }">
        <span class="mono">{{ row.documentId }}</span>
      </template>
    </el-table-column>
    <el-table-column label="实体" width="90" align="right">
      <template #default="{ row }: { row: GraphRagIndexLog }">{{ row.entityCount }}</template>
    </el-table-column>
    <el-table-column label="关系" width="90" align="right">
      <template #default="{ row }: { row: GraphRagIndexLog }">{{ row.relationshipCount }}</template>
    </el-table-column>
    <el-table-column label="Chunk 关联" width="110" align="right">
      <template #default="{ row }: { row: GraphRagIndexLog }">{{ row.chunkEntityCount }}</template>
    </el-table-column>
    <el-table-column label="Neo4j" width="90">
      <template #default="{ row }: { row: GraphRagIndexLog }">
        <el-tag :type="row.neo4jEnabled ? 'success' : 'info'" effect="plain">
          {{ row.neo4jEnabled ? '启用' : '关闭' }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="耗时" width="100" align="right">
      <template #default="{ row }: { row: GraphRagIndexLog }">{{ formatDuration(row.durationMs) }}</template>
    </el-table-column>
    <el-table-column label="创建时间" min-width="170">
      <template #default="{ row }: { row: GraphRagIndexLog }">{{ formatDateTime(row.createdAt) }}</template>
    </el-table-column>
    <el-table-column label="错误摘要" min-width="220">
      <template #default="{ row }: { row: GraphRagIndexLog }">
        <span class="summary-text">{{ row.errorMessage || '-' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="traceId" min-width="230">
      <template #default="{ row }: { row: GraphRagIndexLog }">
        <TraceIdText v-if="row.traceId" :trace-id="row.traceId" />
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import GraphRagStatusTag from '@/features/observability/components/GraphRagStatusTag.vue'
import type { GraphRagIndexLog } from '@/features/observability/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

defineProps<{
  logs: GraphRagIndexLog[]
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
