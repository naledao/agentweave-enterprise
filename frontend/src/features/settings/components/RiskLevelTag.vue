<template>
  <el-tag :type="tag.type" effect="plain">{{ tag.label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { Permission, RiskLevel } from '@/features/auth/types'

const props = defineProps<{
  permission?: Permission
  riskLevel?: RiskLevel
}>()

const normalizedRisk = computed<RiskLevel>(() => {
  if (props.riskLevel) {
    return props.riskLevel
  }

  if (props.permission?.type === 'TOOL') {
    return 'HIGH'
  }

  if (props.permission?.code.includes(':write')) {
    return 'MEDIUM'
  }

  return 'LOW'
})

const tag = computed(() => {
  const map: Record<RiskLevel, { label: string; type: 'success' | 'warning' | 'danger' }> = {
    LOW: { label: '低风险', type: 'success' },
    MEDIUM: { label: '中风险', type: 'warning' },
    HIGH: { label: '高风险', type: 'danger' },
  }

  return map[normalizedRisk.value]
})
</script>
