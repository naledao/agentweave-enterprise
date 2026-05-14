<template>
  <el-tag :type="tag.type" effect="plain">{{ tag.label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { ToolInvocationStatus } from '@/features/tools/types'

const props = defineProps<{
  status: ToolInvocationStatus
}>()

const tag = computed(() => {
  const map: Record<ToolInvocationStatus, { label: string; type: 'info' | 'success' | 'warning' | 'danger' }> = {
    running: { label: '执行中', type: 'info' },
    success: { label: '成功', type: 'success' },
    failed: { label: '失败', type: 'danger' },
    denied: { label: '拒绝', type: 'warning' },
    timeout: { label: '超时', type: 'danger' },
  }

  return map[props.status]
})
</script>
