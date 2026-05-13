<template>
  <el-timeline class="processing-timeline">
    <el-timeline-item
      v-for="step in steps"
      :key="step.status"
      :type="step.type"
      :timestamp="step.timestamp"
    >
      <div class="timeline-step">
        <strong>{{ step.label }}</strong>
        <span>{{ step.description }}</span>
      </div>
    </el-timeline-item>
  </el-timeline>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { DocumentStatus } from '@/features/knowledge/types'
import { documentStatusMeta, type StatusTagType } from '@/features/knowledge/components/documentStatus'

const props = defineProps<{
  status: DocumentStatus
  createdAt: string
  updatedAt: string
  indexedAt: string | null
  errorMessage?: string | null
}>()

const order: DocumentStatus[] = ['uploaded', 'parsing', 'cleaning', 'chunking', 'embedding', 'indexed']

const steps = computed(() => {
  if (props.status === 'failed') {
    return [
      ...completedSteps('failed'),
      {
        status: 'failed',
        label: documentStatusMeta.failed.label,
        description: props.errorMessage || '处理失败',
        timestamp: formatDateTime(props.updatedAt),
        type: documentStatusMeta.failed.type,
      },
    ]
  }

  return completedSteps(props.status)
})

function completedSteps(status: DocumentStatus) {
  const activeIndex = order.includes(status) ? order.indexOf(status) : 0
  return order.slice(0, activeIndex + 1).map((item) => ({
    status: item,
    label: documentStatusMeta[item].label,
    description: description(item),
    timestamp: timestamp(item),
    type: documentStatusMeta[item].type as StatusTagType,
  }))
}

function description(status: DocumentStatus): string {
  const map: Record<DocumentStatus, string> = {
    uploaded: '源文件已保存',
    parsing: '正在解析原始文档',
    cleaning: '正在清洗文本内容',
    chunking: '正在生成文本分段',
    embedding: '正在生成向量并准备入库',
    indexed: '文档已完成入库',
    failed: '处理失败',
  }

  return map[status]
}

function timestamp(status: DocumentStatus): string {
  if (status === 'uploaded') {
    return formatDateTime(props.createdAt)
  }

  if (status === 'indexed' && props.indexedAt) {
    return formatDateTime(props.indexedAt)
  }

  return formatDateTime(props.updatedAt)
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
.processing-timeline {
  padding-left: 4px;
}

.timeline-step {
  display: grid;
  gap: 4px;
}

.timeline-step strong {
  color: #182233;
  font-size: 14px;
}

.timeline-step span {
  color: #69778d;
  font-size: 13px;
  line-height: 1.5;
}
</style>
