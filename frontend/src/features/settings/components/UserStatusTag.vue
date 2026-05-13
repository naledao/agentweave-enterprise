<template>
  <el-tag :type="tag.type" effect="plain">{{ tag.label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { UserStatus } from '@/features/auth/types'

const props = defineProps<{
  status: UserStatus
}>()

const tag = computed(() => {
  const map: Record<UserStatus, { label: string; type: 'success' | 'info' | 'warning' }> = {
    ACTIVE: { label: '启用', type: 'success' },
    DISABLED: { label: '禁用', type: 'info' },
    LOCKED: { label: '锁定', type: 'warning' },
  }

  return map[props.status]
})
</script>
