<template>
  <section class="summary-list">
    <h4>引用资料</h4>
    <div v-if="citations.length === 0" class="list-empty">暂无引用资料</div>
    <article v-for="(citation, index) in citations" v-else :key="citationKey(citation, index)" class="summary-card">
      <div class="card-title">
        <strong>{{ citation.title || citation.documentName || 'Citation' }}</strong>
        <el-tag v-if="citation.score !== null && citation.score !== undefined" effect="plain" type="info">
          score {{ citation.score.toFixed(3) }}
        </el-tag>
      </div>
      <p>{{ citation.snippet }}</p>
      <dl>
        <div v-if="citation.documentId">
          <dt>文档</dt>
          <dd>{{ citation.documentId }}</dd>
        </div>
        <div v-if="citation.chunkId">
          <dt>Chunk</dt>
          <dd>{{ citation.chunkId }}</dd>
        </div>
        <div v-if="citation.source">
          <dt>来源</dt>
          <dd>{{ citation.source }}</dd>
        </div>
      </dl>
    </article>
  </section>
</template>

<script setup lang="ts">
import type { WorkflowCitation } from '@/features/workflows/types'

defineProps<{
  citations: WorkflowCitation[]
}>()

function citationKey(citation: WorkflowCitation, index: number): string {
  return `${citation.documentId ?? 'doc'}-${citation.chunkId ?? index}`
}
</script>

<style scoped>
.summary-list {
  display: grid;
  gap: 10px;
}

.summary-list h4 {
  margin: 0;
  color: #182233;
  font-size: 14px;
}

.list-empty {
  color: #69778d;
  font-size: 13px;
}

.summary-card {
  display: grid;
  gap: 8px;
  border: 1px solid #dfe5ee;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.card-title {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.card-title strong {
  overflow-wrap: anywhere;
}

p {
  display: -webkit-box;
  max-height: 80px;
  overflow: hidden;
  margin: 0;
  color: #39485f;
  font-size: 13px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
}

dl {
  display: grid;
  gap: 6px;
  margin: 0;
}

dl div {
  display: grid;
  min-width: 0;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 8px;
}

dt,
dd {
  margin: 0;
  font-size: 12px;
}

dt {
  color: #69778d;
}

dd {
  overflow: hidden;
  color: #263143;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
