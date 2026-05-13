<template>
  <article class="citation-card">
    <div class="citation-header">
      <CitationSourceLink :citation="citation" />
      <el-tag v-if="citation.score !== undefined" effect="plain" type="info">
        {{ formatScore(citation.score) }}
      </el-tag>
    </div>

    <p class="citation-snippet">{{ citation.snippet }}</p>

    <dl class="citation-meta">
      <div v-if="citation.chunkId">
        <dt>Chunk</dt>
        <dd>{{ citation.chunkId }}</dd>
      </div>
      <div v-if="citation.source">
        <dt>来源</dt>
        <dd>{{ citation.source }}</dd>
      </div>
      <div v-if="citation.businessDomain">
        <dt>业务域</dt>
        <dd>{{ citation.businessDomain }}</dd>
      </div>
      <div v-if="citation.documentType">
        <dt>类型</dt>
        <dd>{{ citation.documentType }}</dd>
      </div>
      <div v-if="citation.permissionLevel">
        <dt>权限</dt>
        <dd>{{ citation.permissionLevel }}</dd>
      </div>
    </dl>
  </article>
</template>

<script setup lang="ts">
import CitationSourceLink from '@/features/chat/components/CitationSourceLink.vue'
import type { RagCitation } from '@/features/chat/types'

defineProps<{
  citation: RagCitation
}>()

function formatScore(score: number): string {
  return `score ${score.toFixed(3)}`
}
</script>

<style scoped>
.citation-card {
  display: grid;
  gap: 10px;
  border: 1px solid #dfe5ee;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.citation-header {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.citation-snippet {
  display: -webkit-box;
  max-height: 84px;
  overflow: hidden;
  margin: 0;
  color: #39485f;
  font-size: 13px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
}

.citation-meta {
  display: grid;
  gap: 8px;
  margin: 0;
}

.citation-meta div {
  display: grid;
  min-width: 0;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 8px;
}

.citation-meta dt,
.citation-meta dd {
  margin: 0;
  font-size: 12px;
  line-height: 1.4;
}

.citation-meta dt {
  color: #69778d;
}

.citation-meta dd {
  overflow: hidden;
  color: #263143;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
