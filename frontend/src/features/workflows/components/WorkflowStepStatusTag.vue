<template>
  <el-tag :type="tag.type" effect="plain">{{ tag.label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import { stepStatusLabel } from '@/features/workflows/components/workflowFormatters'
import type { WorkflowStepStatus } from '@/features/workflows/types'

const props = defineProps<{
  status: WorkflowStepStatus
}>()

const tag = computed(() => {
  const map: Record<WorkflowStepStatus, { label: string; type: 'info' | 'success' | 'warning' | 'danger' }> = {
    PENDING: { label: stepStatusLabel('PENDING'), type: 'info' },
    RUNNING: { label: stepStatusLabel('RUNNING'), type: 'warning' },
    SUCCEEDED: { label: stepStatusLabel('SUCCEEDED'), type: 'success' },
    FAILED: { label: stepStatusLabel('FAILED'), type: 'danger' },
    SKIPPED: { label: stepStatusLabel('SKIPPED'), type: 'info' },
  }

  return map[props.status]
})
</script>
