<template>
  <el-tag :type="tag.type" effect="plain">{{ tag.label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import { runStatusLabel } from '@/features/workflows/components/workflowFormatters'
import type { WorkflowRunStatus } from '@/features/workflows/types'

const props = defineProps<{
  status: WorkflowRunStatus
}>()

const tag = computed(() => {
  const map: Record<WorkflowRunStatus, { label: string; type: 'info' | 'success' | 'warning' | 'danger' }> = {
    CREATED: { label: runStatusLabel('CREATED'), type: 'info' },
    PLANNING: { label: runStatusLabel('PLANNING'), type: 'info' },
    EXECUTING: { label: runStatusLabel('EXECUTING'), type: 'warning' },
    WAITING_APPROVAL: { label: runStatusLabel('WAITING_APPROVAL'), type: 'warning' },
    REVIEWING: { label: runStatusLabel('REVIEWING'), type: 'info' },
    SUCCEEDED: { label: runStatusLabel('SUCCEEDED'), type: 'success' },
    FAILED: { label: runStatusLabel('FAILED'), type: 'danger' },
    CANCELLED: { label: runStatusLabel('CANCELLED'), type: 'info' },
  }

  return map[props.status]
})
</script>
