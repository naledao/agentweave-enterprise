<template>
  <RouterLink
    v-if="citation.documentId"
    class="source-link"
    :to="{ name: 'KnowledgeDocumentDetail', params: { documentId: citation.documentId } }"
  >
    <el-icon><Link /></el-icon>
    {{ citation.documentName ?? citation.title }}
  </RouterLink>
  <span v-else class="source-text">
    {{ citation.documentName ?? citation.title }}
  </span>
</template>

<script setup lang="ts">
import { Link } from '@element-plus/icons-vue'

import type { RagCitation } from '@/features/chat/types'

defineProps<{
  citation: RagCitation
}>()
</script>

<style scoped>
.source-link,
.source-text {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  gap: 4px;
  color: #1f5fbf;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
  overflow-wrap: anywhere;
  text-decoration: none;
}

.source-link:hover {
  text-decoration: underline;
}

.source-text {
  color: #182233;
}
</style>
