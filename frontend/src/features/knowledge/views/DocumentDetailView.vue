<template>
  <section>
    <PageHeader
      :title="documentTitle"
      description="查看文档处理过程、GraphRAG 状态、失败定位信息和 chunk 状态。"
      eyebrow="Knowledge"
    >
      <template #actions>
        <el-button @click="goBack">
          <el-icon><Back /></el-icon>
          返回
        </el-button>
        <el-button
          v-if="canParseDocument"
          type="primary"
          :loading="parseDocumentMutation.isPending.value"
          @click="parseDocument"
        >
          <el-icon><DocumentChecked /></el-icon>
          解析文档
        </el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="detailError"
      class="page-error"
      :title="detailError"
      type="error"
      :closable="false"
      show-icon
    />

    <div v-loading="documentQuery.isFetching.value" class="detail-grid">
      <section class="page-surface summary-panel">
        <div class="summary-header">
          <div>
            <p class="panel-label">文档状态</p>
            <h2>{{ detail?.filename ?? '-' }}</h2>
          </div>
          <DocumentStatusTag v-if="detail" :status="detail.status" />
        </div>

        <dl class="metadata-grid">
          <div>
            <dt>来源</dt>
            <dd>{{ detail?.metadata.source ?? '-' }}</dd>
          </div>
          <div>
            <dt>业务域</dt>
            <dd>{{ detail?.metadata.businessDomain ?? '-' }}</dd>
          </div>
          <div>
            <dt>文档类型</dt>
            <dd>{{ detail?.metadata.documentType ?? '-' }}</dd>
          </div>
          <div>
            <dt>权限级别</dt>
            <dd>{{ detail?.metadata.permissionLevel ?? '-' }}</dd>
          </div>
          <div>
            <dt>Chunk 数量</dt>
            <dd>{{ detail?.chunkCount ?? 0 }}</dd>
          </div>
          <div>
            <dt>更新时间</dt>
            <dd>{{ formatDateTime(detail?.updatedAt ?? null) }}</dd>
          </div>
        </dl>

        <div v-if="detail?.metadata.tags.length" class="tag-row">
          <el-tag
            v-for="tag in detail.metadata.tags"
            :key="tag"
            effect="plain"
          >
            {{ tag }}
          </el-tag>
        </div>

        <el-alert
          v-if="detail?.errorMessage"
          class="status-error"
          :title="detail.errorMessage"
          type="error"
          :closable="false"
          show-icon
        />

        <TraceIdText v-if="detail?.traceId" :trace-id="detail.traceId" />
      </section>

      <div class="sidebar-panels">
        <section class="page-surface timeline-panel">
          <h3>处理时间线</h3>
          <DocumentProcessingTimeline
            v-if="detail"
            :status="detail.status"
            :created-at="detail.createdAt"
            :updated-at="detail.updatedAt"
            :indexed-at="detail.indexedAt"
            :error-message="detail.errorMessage"
          />
        </section>

        <section class="page-surface graph-panel">
          <GraphRagIndexPanel
            v-if="detail"
            :graph-rag="detail.graphRag"
            :rebuild-disabled="isProcessingDocument"
            :rebuilding="reindexDocumentMutation.isPending.value"
            @rebuild="confirmReindexDocument"
          />
        </section>
      </div>

      <section class="page-surface chunk-panel">
        <div class="panel-heading">
          <h3>Chunk 状态</h3>
          <span class="muted-text">{{ detail?.chunks.length ?? 0 }} 个分段</span>
        </div>
        <DocumentChunkList :chunks="detail?.chunks ?? []" />
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { Back, DocumentChecked } from '@element-plus/icons-vue'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { documentsApi } from '@/features/knowledge/api/documentsApi'
import GraphRagIndexPanel from '@/features/knowledge/components/GraphRagIndexPanel.vue'
import DocumentChunkList from '@/features/knowledge/components/DocumentChunkList.vue'
import DocumentProcessingTimeline from '@/features/knowledge/components/DocumentProcessingTimeline.vue'
import DocumentStatusTag from '@/features/knowledge/components/DocumentStatusTag.vue'
import PageHeader from '@/shared/components/PageHeader.vue'
import TraceIdText from '@/shared/components/TraceIdText.vue'
import { formatApiError } from '@/shared/utils/apiError'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()

const documentId = computed(() => {
  const id = route.params.documentId
  return typeof id === 'string' ? id : ''
})

const documentQuery = useQuery({
  queryKey: computed(() => ['knowledge-document-detail', documentId.value]),
  queryFn: () => documentsApi.getDocument(documentId.value),
  enabled: computed(() => Boolean(documentId.value)),
})

const detail = computed(() => documentQuery.data.value ?? null)
const documentTitle = computed(() => detail.value?.filename ?? '文档详情')
const canParseDocument = computed(() => detail.value?.status === 'uploaded')
const isProcessingDocument = computed(() => {
  const status = detail.value?.status
  return status === 'parsing'
    || status === 'cleaning'
    || status === 'chunking'
    || status === 'embedding'
})

const detailError = computed(() => {
  if (!documentQuery.isError.value || !documentQuery.error.value) {
    return ''
  }

  return formatApiError(documentQuery.error.value)
})

const parseDocumentMutation = useMutation({
  mutationFn: () => documentsApi.parseDocument(documentId.value),
  async onSuccess() {
    ElMessage.success('文档解析已完成')
    await queryClient.invalidateQueries({ queryKey: ['knowledge-document-detail', documentId.value] })
    await queryClient.invalidateQueries({ queryKey: ['knowledge-documents'] })
  },
  onError(error) {
    ElMessage.error(formatApiError(error))
    void queryClient.invalidateQueries({ queryKey: ['knowledge-document-detail', documentId.value] })
  },
})

const reindexDocumentMutation = useMutation({
  mutationFn: () => documentsApi.reindexDocument(documentId.value),
  async onSuccess() {
    ElMessage.success('图谱索引已重建')
    await queryClient.invalidateQueries({ queryKey: ['knowledge-document-detail', documentId.value] })
    await queryClient.invalidateQueries({ queryKey: ['knowledge-documents'] })
  },
  onError(error) {
    ElMessage.error(formatApiError(error))
    void queryClient.invalidateQueries({ queryKey: ['knowledge-document-detail', documentId.value] })
  },
})

function parseDocument(): void {
  if (!documentId.value) {
    return
  }
  parseDocumentMutation.mutate()
}

async function confirmReindexDocument(): Promise<void> {
  if (!documentId.value || !detail.value) {
    return
  }
  await ElMessageBox.confirm(
    `确认重新为 ${detail.value.filename} 构建图谱索引？`,
    '重建图谱索引',
    { type: 'warning' },
  )
  reindexDocumentMutation.mutate()
}

async function goBack(): Promise<void> {
  await router.push({ name: 'KnowledgeDocuments' })
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
</script>

<style scoped>
.page-error {
  margin-bottom: 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.8fr);
  gap: 16px;
  align-items: start;
}

.summary-panel,
.timeline-panel,
.graph-panel,
.chunk-panel {
  padding: 18px;
}

.summary-header,
.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-label {
  margin: 0 0 4px;
  color: #69778d;
  font-size: 12px;
}

h2,
h3 {
  margin: 0;
  color: #182233;
}

h2 {
  overflow-wrap: anywhere;
  font-size: 18px;
}

h3 {
  margin-bottom: 14px;
  font-size: 15px;
}

.metadata-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin: 18px 0 0;
}

.metadata-grid div {
  min-width: 0;
}

.metadata-grid dt {
  color: #69778d;
  font-size: 12px;
}

.metadata-grid dd {
  overflow: hidden;
  margin: 4px 0 0;
  color: #263143;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.status-error {
  margin-top: 16px;
}

.summary-panel :deep(.trace-id-text) {
  margin-top: 14px;
}

.timeline-panel {
  align-self: start;
}

.sidebar-panels {
  display: grid;
  gap: 16px;
  align-self: start;
}

.chunk-panel {
  grid-column: 1 / -1;
}

.panel-heading {
  align-items: center;
  margin-bottom: 12px;
}

.panel-heading h3 {
  margin-bottom: 0;
}

@media (max-width: 980px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .metadata-grid {
    grid-template-columns: 1fr 1fr;
  }

  .sidebar-panels {
    grid-column: 1;
  }
}

@media (max-width: 640px) {
  .metadata-grid {
    grid-template-columns: 1fr;
  }
}
</style>
