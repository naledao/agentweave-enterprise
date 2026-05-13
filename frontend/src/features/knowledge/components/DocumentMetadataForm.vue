<template>
  <div class="metadata-form-grid">
    <el-form-item label="来源" prop="source">
      <el-input
        :model-value="modelValue.source"
        :disabled="disabled"
        maxlength="160"
        show-word-limit
        @update:model-value="updateSource"
      />
    </el-form-item>

    <el-form-item label="业务域" prop="businessDomain">
      <el-input
        :model-value="modelValue.businessDomain"
        :disabled="disabled"
        maxlength="120"
        show-word-limit
        @update:model-value="updateBusinessDomain"
      />
    </el-form-item>

    <el-form-item label="文档类型" prop="documentType">
      <el-select
        :model-value="modelValue.documentType"
        allow-create
        default-first-option
        filterable
        :disabled="disabled"
        @update:model-value="updateDocumentType"
      >
        <el-option
          v-for="option in documentTypeOptions"
          :key="option"
          :label="option"
          :value="option"
        />
      </el-select>
    </el-form-item>

    <el-form-item label="权限级别" prop="permissionLevel">
      <el-select
        :model-value="modelValue.permissionLevel"
        allow-create
        default-first-option
        filterable
        :disabled="disabled"
        @update:model-value="updatePermissionLevel"
      >
        <el-option
          v-for="option in permissionLevelOptions"
          :key="option"
          :label="option"
          :value="option"
        />
      </el-select>
    </el-form-item>

    <el-form-item label="生效时间" prop="effectiveFrom">
      <el-date-picker
        :model-value="modelValue.effectiveFrom"
        :disabled="disabled"
        type="datetime"
        @update:model-value="updateEffectiveFrom"
      />
    </el-form-item>

    <el-form-item label="失效时间" prop="effectiveTo">
      <el-date-picker
        :model-value="modelValue.effectiveTo"
        :disabled="disabled"
        type="datetime"
        @update:model-value="updateEffectiveTo"
      />
    </el-form-item>

    <el-form-item class="metadata-form-grid__wide" label="标签" prop="tags">
      <el-select
        :model-value="modelValue.tags"
        allow-create
        default-first-option
        filterable
        multiple
        :disabled="disabled"
        @update:model-value="updateTags"
      >
        <el-option
          v-for="option in tagOptions"
          :key="option"
          :label="option"
          :value="option"
        />
      </el-select>
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import type { DocumentMetadataFormModel } from '@/features/knowledge/types'

const props = defineProps<{
  modelValue: DocumentMetadataFormModel
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [model: DocumentMetadataFormModel]
}>()

const documentTypeOptions = ['RUNBOOK', 'API_DOC', 'FAQ', 'MANUAL', 'TICKET']
const permissionLevelOptions = ['PUBLIC', 'INTERNAL', 'CONFIDENTIAL']
const tagOptions = ['api', 'runbook', 'faq', 'incident', 'troubleshooting']

function updateField<K extends keyof DocumentMetadataFormModel>(
  key: K,
  value: DocumentMetadataFormModel[K],
): void {
  emit('update:modelValue', {
    ...props.modelValue,
    [key]: value,
  } as DocumentMetadataFormModel)
}

function updateSource(value: string): void {
  updateField('source', value)
}

function updateBusinessDomain(value: string): void {
  updateField('businessDomain', value)
}

function updateDocumentType(value: string): void {
  updateField('documentType', value)
}

function updatePermissionLevel(value: string): void {
  updateField('permissionLevel', value)
}

function updateEffectiveFrom(value: string | number | Date | null): void {
  updateField('effectiveFrom', normalizeDateValue(value))
}

function updateEffectiveTo(value: string | number | Date | null): void {
  updateField('effectiveTo', normalizeDateValue(value))
}

function updateTags(value: string[] | string): void {
  updateField('tags', normalizeTagsValue(value))
}

function normalizeDateValue(value: string | number | Date | null): Date | null {
  if (!value) {
    return null
  }
  return value instanceof Date ? value : new Date(value)
}

function normalizeTagsValue(value: string[] | string): string[] {
  return Array.isArray(value) ? value : [value]
}
</script>

<style scoped>
.metadata-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 14px;
}

.metadata-form-grid__wide {
  grid-column: 1 / -1;
}

.metadata-form-grid :deep(.el-select),
.metadata-form-grid :deep(.el-date-editor) {
  width: 100%;
}

@media (max-width: 640px) {
  .metadata-form-grid {
    grid-template-columns: 1fr;
  }

  .metadata-form-grid__wide {
    grid-column: auto;
  }
}
</style>
