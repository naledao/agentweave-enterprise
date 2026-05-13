<template>
  <section class="panel-section">
    <h3>引用资料</h3>
    <div v-if="citations.length === 0" class="panel-empty">暂无引用</div>
    <article v-for="citation in citations" v-else :key="citation.chunkId ?? citation.documentId ?? citation.title" class="citation-card">
      <strong>{{ citation.title }}</strong>
      <p>{{ citation.snippet }}</p>
      <span v-if="citation.score !== undefined" class="muted-text">score: {{ citation.score.toFixed(3) }}</span>
    </article>
  </section>
</template>

<script setup lang="ts">
import type { Citation } from '@/features/chat/types'

defineProps<{
  citations: Citation[]
}>()
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

.panel-empty {
  color: #69778d;
  font-size: 13px;
}

.citation-card {
  display: grid;
  gap: 8px;
  border: 1px solid #dfe5ee;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.citation-card p {
  margin: 0;
  color: #39485f;
  font-size: 13px;
  line-height: 1.6;
}
</style>
