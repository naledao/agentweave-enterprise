<template>
  <article class="graph-path-card">
    <div class="graph-path-title">
      <strong>{{ graphPath.pathId ?? 'Graph path' }}</strong>
      <div class="path-tags">
        <el-tag effect="plain" type="info">{{ graphPath.depth }} hop</el-tag>
        <el-tag v-if="graphPath.confidence !== undefined" effect="plain" type="success">
          {{ formatConfidence(graphPath.confidence) }}
        </el-tag>
      </div>
    </div>

    <div v-if="graphPath.entities.length" class="path-line" aria-label="graph entities">
      <template v-for="(entity, index) in graphPath.entities" :key="`${entity}-${index}`">
        <span class="path-entity">{{ entity }}</span>
        <span v-if="index < graphPath.relationships.length" class="path-relationship">
          {{ graphPath.relationships[index] }}
        </span>
      </template>
    </div>

    <p v-if="graphPath.relationships.length" class="path-summary">
      {{ graphPath.relationships.join(' -> ') }}
    </p>

    <p v-if="graphPath.sourceChunkIds.length" class="muted-text">
      chunks: {{ graphPath.sourceChunkIds.join(', ') }}
    </p>
  </article>
</template>

<script setup lang="ts">
import type { RagGraphPath } from '@/features/chat/types'

defineProps<{
  graphPath: RagGraphPath
}>()

function formatConfidence(confidence: number): string {
  return `confidence ${confidence.toFixed(3)}`
}
</script>

<style scoped>
.graph-path-card {
  display: grid;
  gap: 10px;
  border: 1px solid #dfe5ee;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.graph-path-title {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.graph-path-title strong {
  overflow-wrap: anywhere;
}

.path-tags {
  display: inline-flex;
  flex: none;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.path-line {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.path-entity,
.path-relationship {
  max-width: 100%;
  border: 1px solid #cbd6e4;
  border-radius: 6px;
  background: #f6f8fb;
  padding: 3px 7px;
  color: #223149;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.path-relationship {
  border-color: #d2dfcc;
  background: #f4faf1;
  color: #436b35;
}

.path-summary {
  margin: 0;
  color: #39485f;
  font-size: 13px;
  line-height: 1.6;
}

.muted-text {
  margin: 0;
  font-size: 12px;
  overflow-wrap: anywhere;
}
</style>
