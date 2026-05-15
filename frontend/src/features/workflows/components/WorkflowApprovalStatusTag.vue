<template>
  <el-tag :type="tag.type" effect="plain">{{ tag.label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import { approvalStatusLabel } from '@/features/workflows/components/workflowFormatters'
import type { WorkflowApprovalStatus } from '@/features/workflows/types'

const props = defineProps<{
  status: WorkflowApprovalStatus
}>()

const tag = computed(() => {
  const map: Record<WorkflowApprovalStatus, { label: string; type: 'info' | 'success' | 'warning' | 'danger' }> = {
    PENDING: { label: approvalStatusLabel('PENDING'), type: 'warning' },
    APPROVED: { label: approvalStatusLabel('APPROVED'), type: 'success' },
    REJECTED: { label: approvalStatusLabel('REJECTED'), type: 'danger' },
    CANCELLED: { label: approvalStatusLabel('CANCELLED'), type: 'info' },
  }

  return map[props.status]
})
</script>
