<template>
  <section class="panel-section">
    <div class="panel-title-row">
      <h3>引用资料</h3>
      <el-tag v-if="citations.length" effect="plain" type="info">{{ citations.length }}</el-tag>
    </div>
    <div v-if="citations.length === 0" class="panel-empty">{{ emptyText }}</div>
    <CitationCard
      v-for="citation in citations"
      v-else
      :key="citationKey(citation)"
      :citation="citation"
    />
  </section>
</template>

<script setup lang="ts">
import CitationCard from '@/features/chat/components/CitationCard.vue'
import type { RagCitation } from '@/features/chat/types'

withDefaults(defineProps<{
  citations: RagCitation[]
  emptyText?: string
}>(), {
  emptyText: '暂无引用',
})

function citationKey(citation: RagCitation): string {
  if (citation.chunkId) {
    return `chunk:${citation.chunkId}`
  }
  if (citation.documentId) {
    return `document:${citation.documentId}:${citation.snippet}`
  }
  return `${citation.title}:${citation.snippet}`
}

defineExpose({
  citationKey,
})
</script>

<style scoped>
.panel-section {
  display: grid;
  gap: 10px;
}

.panel-section h3 {
  margin: 0;
  color: #182233;
  font-size: 15px;
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.panel-empty {
  color: #69778d;
  font-size: 13px;
}
</style>
