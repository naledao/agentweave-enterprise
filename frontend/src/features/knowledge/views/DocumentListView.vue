<template>
  <section class="document-list-view">
    <PageHeader
      title="知识库文档"
      description="查看文档入库状态、失败原因和 chunk 处理进度。"
      eyebrow="Knowledge"
    >
      <template #actions>
        <el-button type="primary" @click="openUploadDrawer">
          <el-icon><Upload /></el-icon>
          上传文档
        </el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="listError"
      class="page-error"
      :title="listError"
      type="error"
      :closable="false"
      show-icon
    />

    <div class="page-toolbar">
      <div class="toolbar-filters">
        <el-input
          v-model="keywordInput"
          clearable
          placeholder="搜索文档名称"
          style="width: 260px"
          @keyup.enter="searchDocuments"
          @clear="searchDocuments"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-button @click="searchDocuments">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
      </div>
    </div>

    <div class="page-surface document-table-surface">
      <DocumentTable
        class="document-table"
        :documents="documents"
        :loading="documentsQuery.isFetching.value"
        @open="openDocument"
        @reindex="confirmReindexDocument"
        @delete="confirmDeleteDocument"
      />

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </div>

    <el-drawer
      v-model="uploadDrawerVisible"
      destroy-on-close
      size="560px"
      title="上传文档"
    >
      <DocumentUploadForm
        :error-message="uploadError.message"
        :submitting="uploadDocumentMutation.isPending.value"
        :trace-id="uploadError.traceId"
        :upload-progress="uploadProgress"
        @cancel="closeUploadDrawer"
        @submit="uploadDocument"
      />
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { Search, Upload } from '@element-plus/icons-vue'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { knowledgeDocumentApi } from '@/features/knowledge/api/documentsApi'
import DocumentTable from '@/features/knowledge/components/DocumentTable.vue'
import DocumentUploadForm from '@/features/knowledge/components/DocumentUploadForm.vue'
import type { KnowledgeDocument, KnowledgeDocumentUploadPayload } from '@/features/knowledge/types'
import PageHeader from '@/shared/components/PageHeader.vue'
import { formatApiError, getApiErrorDisplay } from '@/shared/utils/apiError'

const router = useRouter()
const queryClient = useQueryClient()
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const keywordInput = ref('')
const uploadDrawerVisible = ref(false)
const uploadProgress = ref(0)
const uploadError = ref<{ message: string; traceId: string | null }>({
  message: '',
  traceId: null,
})

const documentsQuery = useQuery({
  queryKey: computed(() => [
    'knowledge-documents',
    {
      page: page.value - 1,
      size: size.value,
      keyword: keyword.value || undefined,
    },
  ]),
  queryFn: () => knowledgeDocumentApi.list({
    page: page.value - 1,
    size: size.value,
    keyword: keyword.value || undefined,
  }),
})

const documents = computed(() => documentsQuery.data.value?.items ?? [])
const total = computed(() => documentsQuery.data.value?.total ?? 0)
const listError = computed(() => {
  if (!documentsQuery.isError.value || !documentsQuery.error.value) {
    return ''
  }

  return formatApiError(documentsQuery.error.value)
})

const uploadDocumentMutation = useMutation({
  mutationFn: (payload: KnowledgeDocumentUploadPayload) => {
    uploadProgress.value = 0
    uploadError.value = { message: '', traceId: null }
    return knowledgeDocumentApi.upload(payload, (progress) => {
      uploadProgress.value = progress
    })
  },
  async onSuccess() {
    ElMessage.success('文档已上传')
    closeUploadDrawer()
    page.value = 1
    await queryClient.invalidateQueries({ queryKey: ['knowledge-documents'] })
  },
  onError(error) {
    uploadError.value = getApiErrorDisplay(error, '文档上传失败')
    ElMessage.error(uploadError.value.message)
  },
})

const reindexDocumentMutation = useMutation({
  mutationFn: (documentId: string) => knowledgeDocumentApi.reindex(documentId),
  async onSuccess() {
    ElMessage.success('Reindex completed')
    await queryClient.invalidateQueries({ queryKey: ['knowledge-documents'] })
  },
  onError(error) {
    ElMessage.error(formatApiError(error))
    void queryClient.invalidateQueries({ queryKey: ['knowledge-documents'] })
  },
})

const deleteDocumentMutation = useMutation({
  mutationFn: (documentId: string) => knowledgeDocumentApi.delete(documentId),
  async onSuccess() {
    ElMessage.success('文档已删除')
    await queryClient.invalidateQueries({ queryKey: ['knowledge-documents'] })
  },
  onError(error) {
    ElMessage.error(formatApiError(error))
    void queryClient.invalidateQueries({ queryKey: ['knowledge-documents'] })
  },
})

watch(size, () => {
  page.value = 1
})

function openUploadDrawer(): void {
  uploadError.value = { message: '', traceId: null }
  uploadProgress.value = 0
  uploadDrawerVisible.value = true
}

function closeUploadDrawer(): void {
  uploadDrawerVisible.value = false
  uploadProgress.value = 0
}

function uploadDocument(payload: KnowledgeDocumentUploadPayload): void {
  uploadDocumentMutation.mutate(payload)
}

function searchDocuments(): void {
  keyword.value = keywordInput.value.trim()
  page.value = 1
}

async function openDocument(document: KnowledgeDocument): Promise<void> {
  await router.push({ name: 'KnowledgeDocumentDetail', params: { documentId: document.documentId } })
}

async function confirmReindexDocument(document: KnowledgeDocument): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `Reindex ${document.filename}?`,
      'Reindex document',
      { type: 'warning' },
    )
  } catch {
    return
  }
  reindexDocumentMutation.mutate(document.documentId)
}

async function confirmDeleteDocument(document: KnowledgeDocument): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `删除 ${document.filename}?`,
      '删除文档',
      { type: 'warning' },
    )
  } catch {
    return
  }
  deleteDocumentMutation.mutate(document.documentId)
}
</script>

<style scoped>
.document-list-view {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.page-error {
  flex: none;
  margin-bottom: 16px;
}

.page-toolbar {
  flex: none;
}

.document-table-surface {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
}

.document-table {
  min-height: 0;
  flex: 1;
}

.pagination-row {
  display: flex;
  flex: none;
  justify-content: flex-end;
  border-top: 1px solid #edf1f5;
  padding: 14px 16px;
}
</style>
