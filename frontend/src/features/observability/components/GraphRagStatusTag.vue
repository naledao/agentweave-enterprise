<template>
  <el-tag :type="tag.type" effect="plain">{{ tag.label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type {
  GraphRagIndexStatus,
  GraphRagRetrievalStatus,
} from '@/features/observability/types'

type Status = GraphRagIndexStatus | GraphRagRetrievalStatus | string
type TagType = 'info' | 'success' | 'warning' | 'danger'

const props = defineProps<{
  status: Status
}>()

const tag = computed((): { label: string; type: TagType } => {
  const map: Record<string, { label: string; type: TagType }> = {
    PROCESSING: { label: '处理中', type: 'warning' },
    INDEXED: { label: '已构建', type: 'success' },
    SKIPPED: { label: '已跳过', type: 'info' },
    SUCCESS: { label: '成功', type: 'success' },
    FAILED: { label: '失败', type: 'danger' },
    DEGRADED: { label: '降级', type: 'warning' },
  }

  return map[props.status] ?? { label: props.status, type: 'info' }
})
</script>
