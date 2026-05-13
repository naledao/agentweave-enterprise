<template>
  <el-table
    v-loading="loading"
    class="document-table"
    :data="documents"
    height="100%"
    row-key="documentId"
    empty-text="暂无文档"
  >
    <el-table-column label="文档" min-width="240">
      <template #default="{ row }: { row: KnowledgeDocument }">
        <div class="document-title">
          <strong>{{ row.filename }}</strong>
          <span>{{ row.contentType }} · {{ formatFileSize(row.fileSize) }}</span>
        </div>
      </template>
    </el-table-column>
    <el-table-column label="业务域" min-width="120">
      <template #default="{ row }: { row: KnowledgeDocument }">
        {{ row.metadata.businessDomain }}
      </template>
    </el-table-column>
    <el-table-column label="文档类型" min-width="120">
      <template #default="{ row }: { row: KnowledgeDocument }">
        {{ row.metadata.documentType }}
      </template>
    </el-table-column>
    <el-table-column label="权限级别" min-width="110">
      <template #default="{ row }: { row: KnowledgeDocument }">
        {{ row.metadata.permissionLevel }}
      </template>
    </el-table-column>
    <el-table-column label="上传人" min-width="180">
      <template #default="{ row }: { row: KnowledgeDocument }">
        <span class="monospace-text">{{ row.uploadedBy }}</span>
      </template>
    </el-table-column>
    <el-table-column label="状态" width="110">
      <template #default="{ row }: { row: KnowledgeDocument }">
        <DocumentStatusTag :status="row.status" />
      </template>
    </el-table-column>
    <el-table-column label="Chunk" width="90" align="right">
      <template #default="{ row }: { row: KnowledgeDocument }">
        {{ row.chunkCount }}
      </template>
    </el-table-column>
    <el-table-column label="更新时间" min-width="170">
      <template #default="{ row }: { row: KnowledgeDocument }">
        {{ formatDateTime(row.updatedAt) }}
      </template>
    </el-table-column>
    <el-table-column label="失败摘要" min-width="220">
      <template #default="{ row }: { row: KnowledgeDocument }">
        <div class="failure-cell">
          <span class="error-summary">{{ row.errorMessage || '-' }}</span>
          <TraceIdText v-if="row.traceId && row.status === 'failed'" :trace-id="row.traceId" />
        </div>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="210" fixed="right">
      <template #default="{ row }: { row: KnowledgeDocument }">
        <div class="table-actions">
          <el-button size="small" @click="$emit('open', row)">详情</el-button>
          <el-button
            size="small"
            type="primary"
            :disabled="isProcessing(row)"
            @click="$emit('reindex', row)"
          >
            Reindex
          </el-button>
          <el-button
            size="small"
            type="danger"
            :disabled="isProcessing(row)"
            @click="$emit('delete', row)"
          >
            删除
          </el-button>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import type { KnowledgeDocument } from '@/features/knowledge/types'
import DocumentStatusTag from '@/features/knowledge/components/DocumentStatusTag.vue'
import TraceIdText from '@/shared/components/TraceIdText.vue'

defineProps<{
  documents: KnowledgeDocument[]
  loading?: boolean
}>()

defineEmits<{
  open: [document: KnowledgeDocument]
  reindex: [document: KnowledgeDocument]
  delete: [document: KnowledgeDocument]
}>()

function isProcessing(document: KnowledgeDocument): boolean {
  return ['parsing', 'cleaning', 'chunking', 'embedding'].includes(document.status)
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

function formatFileSize(value: number): string {
  if (value < 1024) {
    return `${value} B`
  }

  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }

  return `${(value / 1024 / 1024).toFixed(1)} MB`
}
</script>

<style scoped>
.document-table {
  width: 100%;
}

.document-table :deep(.el-table__inner-wrapper) {
  height: 100%;
}

.document-title {
  display: grid;
  gap: 4px;
}

.document-title strong {
  max-width: 100%;
  overflow: hidden;
  color: #182233;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-title span,
.error-summary {
  color: #69778d;
  font-size: 12px;
}

.failure-cell {
  display: grid;
  gap: 4px;
}

.monospace-text {
  display: inline-block;
  max-width: 150px;
  overflow: hidden;
  color: #5b6b84;
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.error-summary {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
