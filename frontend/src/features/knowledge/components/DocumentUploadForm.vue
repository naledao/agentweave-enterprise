<template>
  <el-form
    ref="formRef"
    class="document-upload-form"
    :model="form"
    :rules="rules"
    label-position="top"
    @submit.prevent
  >
    <el-form-item label="文档文件" prop="file">
      <el-upload
        v-model:file-list="fileList"
        accept=".pdf,.docx,.txt,.md,.csv,.xlsx"
        :auto-upload="false"
        drag
        :disabled="submitting"
        :limit="1"
        :on-change="handleFileChange"
        :on-exceed="handleFileExceed"
        :on-remove="handleFileRemove"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div>选择或拖拽文件</div>
      </el-upload>
    </el-form-item>

    <DocumentMetadataForm v-model="metadataModel" :disabled="submitting" />

    <el-progress
      v-if="uploadProgressVisible"
      :percentage="uploadProgress"
      :status="uploadProgress === 100 ? 'success' : undefined"
    />

    <el-alert
      v-if="errorMessage"
      class="upload-error"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    />

    <TraceIdText v-if="traceId" :trace-id="traceId" />

    <div class="form-actions">
      <el-button :disabled="submitting" @click="$emit('cancel')">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">
        <el-icon><Upload /></el-icon>
        上传
      </el-button>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { Upload, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules, type UploadFile, type UploadUserFile } from 'element-plus'
import { computed, reactive, ref } from 'vue'

import DocumentMetadataForm from '@/features/knowledge/components/DocumentMetadataForm.vue'
import type {
  DocumentMetadataFormModel,
  KnowledgeDocumentUploadPayload,
} from '@/features/knowledge/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

const MAX_FILE_SIZE = 50 * 1024 * 1024
const allowedExtensions = ['pdf', 'docx', 'txt', 'md', 'csv', 'xlsx']
const allowedContentTypes = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/plain',
  'text/markdown',
  'text/csv',
  'application/csv',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
]

const props = withDefaults(defineProps<{
  submitting?: boolean
  uploadProgress?: number
  errorMessage?: string
  traceId?: string | null
}>(), {
  submitting: false,
  uploadProgress: 0,
  errorMessage: '',
  traceId: null,
})

const emit = defineEmits<{
  cancel: []
  submit: [payload: KnowledgeDocumentUploadPayload]
}>()

interface UploadFormModel extends DocumentMetadataFormModel {
  file: File | null
}

const formRef = ref<FormInstance>()
const fileList = ref<UploadUserFile[]>([])
const form = reactive<UploadFormModel>({
  file: null,
  source: '',
  businessDomain: '',
  documentType: '',
  permissionLevel: '',
  effectiveFrom: null,
  effectiveTo: null,
  tags: [],
})

const rules: FormRules<UploadFormModel> = {
  file: [
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback(new Error('请选择文档文件'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
  source: [{ required: true, message: '请输入来源', trigger: 'blur' }],
  businessDomain: [{ required: true, message: '请输入业务域', trigger: 'blur' }],
  documentType: [{ required: true, message: '请输入文档类型', trigger: 'change' }],
  permissionLevel: [{ required: true, message: '请输入权限级别', trigger: 'change' }],
  effectiveTo: [
    {
      validator: (_rule, value: Date | null, callback) => {
        if (form.effectiveFrom && value && form.effectiveFrom > value) {
          callback(new Error('失效时间不能早于生效时间'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
}

const metadataModel = computed<DocumentMetadataFormModel>({
  get() {
    return {
      source: form.source,
      businessDomain: form.businessDomain,
      documentType: form.documentType,
      permissionLevel: form.permissionLevel,
      effectiveFrom: form.effectiveFrom,
      effectiveTo: form.effectiveTo,
      tags: form.tags,
    }
  },
  set(value) {
    form.source = value.source
    form.businessDomain = value.businessDomain
    form.documentType = value.documentType
    form.permissionLevel = value.permissionLevel
    form.effectiveFrom = value.effectiveFrom
    form.effectiveTo = value.effectiveTo
    form.tags = value.tags
  },
})

const uploadProgressVisible = computed(() => props.submitting || props.uploadProgress > 0)
const uploadProgress = computed(() => Math.min(100, Math.max(0, props.uploadProgress)))

function handleFileChange(file: UploadFile): void {
  if (!file.raw) {
    return
  }

  const message = validateFile(file.raw)
  if (message) {
    ElMessage.error(message)
    fileList.value = []
    form.file = null
    return
  }

  form.file = file.raw
  void formRef.value?.validateField('file')
}

function handleFileRemove(): void {
  form.file = null
  void formRef.value?.validateField('file')
}

function handleFileExceed(): void {
  ElMessage.warning('一次只能上传一个文件')
}

async function submit(): Promise<void> {
  if (!formRef.value) {
    return
  }

  let valid = false
  try {
    valid = await formRef.value.validate()
  } catch {
    return
  }

  if (!valid || !form.file) {
    return
  }

  emit('submit', {
    file: form.file,
    source: form.source.trim(),
    businessDomain: form.businessDomain.trim(),
    documentType: form.documentType.trim(),
    permissionLevel: form.permissionLevel.trim(),
    effectiveFrom: toIsoString(form.effectiveFrom),
    effectiveTo: toIsoString(form.effectiveTo),
    tags: normalizeTags(form.tags),
  })
}

function validateFile(file: File): string {
  if (file.size > MAX_FILE_SIZE) {
    return '文件大小不能超过 50MB'
  }

  const extension = file.name.split('.').pop()?.toLowerCase() ?? ''
  if (!allowedExtensions.includes(extension)) {
    return '文件类型仅支持 pdf、docx、txt、md、csv、xlsx'
  }

  if (file.type && !allowedContentTypes.includes(file.type)) {
    return '文件内容类型不受支持'
  }

  return ''
}

function normalizeTags(tags: string[]): string[] {
  return tags
    .map((tag) => tag.trim())
    .filter(Boolean)
    .filter((tag, index, list) => list.indexOf(tag) === index)
    .slice(0, 20)
}

function toIsoString(value: Date | null): string | null {
  return value ? value.toISOString() : null
}
</script>

<style scoped>
.document-upload-form {
  display: grid;
  gap: 4px;
}

.document-upload-form :deep(.el-upload),
.document-upload-form :deep(.el-upload-dragger) {
  width: 100%;
}

.upload-icon {
  margin-bottom: 8px;
  color: #4b6fd6;
  font-size: 34px;
}

.upload-error {
  margin-top: 8px;
}

.document-upload-form :deep(.trace-id-text) {
  margin-top: 6px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
}
</style>
