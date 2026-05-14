<template>
  <el-tag :type="tag.type" effect="plain">{{ tag.label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { ToolRiskLevel } from '@/features/tools/types'

const props = defineProps<{
  riskLevel: ToolRiskLevel
}>()

const tag = computed(() => {
  const map: Record<ToolRiskLevel, { label: string; type: 'success' | 'warning' | 'danger' }> = {
    LOW: { label: '低风险', type: 'success' },
    MEDIUM: { label: '中风险', type: 'warning' },
    HIGH: { label: '高风险', type: 'danger' },
  }

  return map[props.riskLevel]
})
</script>
