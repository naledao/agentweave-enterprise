<template>
  <el-table
    :data="chunks"
    row-key="chunkId"
    empty-text="暂无 chunk"
  >
    <el-table-column label="#" width="70">
      <template #default="{ row }: { row: DocumentChunk }">
        {{ row.chunkIndex + 1 }}
      </template>
    </el-table-column>
    <el-table-column label="内容摘要" min-width="360">
      <template #default="{ row }: { row: DocumentChunk }">
        <p class="chunk-content">{{ row.content }}</p>
      </template>
    </el-table-column>
    <el-table-column label="长度" width="90" align="right">
      <template #default="{ row }: { row: DocumentChunk }">
        {{ row.contentLength }}
      </template>
    </el-table-column>
    <el-table-column label="状态" width="120">
      <template #default="{ row }: { row: DocumentChunk }">
        <el-tag :type="chunkStatusMeta(row.status).type" effect="plain">
          {{ chunkStatusMeta(row.status).label }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="错误摘要" min-width="220">
      <template #default="{ row }: { row: DocumentChunk }">
        <span class="error-summary">{{ row.errorMessage || '-' }}</span>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import type { DocumentChunk, DocumentChunkStatus } from '@/features/knowledge/types'
import { documentChunkStatusMeta } from '@/features/knowledge/components/documentStatus'

defineProps<{
  chunks: DocumentChunk[]
}>()

function chunkStatusMeta(status: DocumentChunkStatus) {
  return documentChunkStatusMeta[status] ?? { label: status, type: 'info' as const }
}
</script>

<style scoped>
.chunk-content {
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

.error-summary {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #69778d;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
