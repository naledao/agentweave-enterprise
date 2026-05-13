<template>
  <el-tag :type="tag.type" effect="plain">{{ tag.label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { DocumentStatus } from '@/features/knowledge/types'
import { documentStatusMeta } from '@/features/knowledge/components/documentStatus'

const props = defineProps<{
  status: DocumentStatus
}>()

const tag = computed(() => documentStatusMeta[props.status] ?? { label: props.status, type: 'info' })
</script>
