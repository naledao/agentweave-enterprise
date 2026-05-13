<template>
  <section class="panel-section">
    <h3>图谱路径</h3>
    <div v-if="graphPaths.length === 0" class="panel-empty">暂无图谱路径</div>
    <article
      v-for="graphPath in graphPaths"
      v-else
      :key="graphPath.pathId ?? graphPath.entities.join('>')"
      class="graph-path-card"
    >
      <div class="graph-path-title">
        <strong>{{ graphPath.pathId ?? 'Graph path' }}</strong>
        <el-tag effect="plain" type="info">{{ graphPath.depth }} hop</el-tag>
      </div>
      <div v-if="graphPath.entities.length" class="path-line">
        <span
          v-for="(entity, index) in graphPath.entities"
          :key="`${entity}-${index}`"
          class="path-entity"
        >
          {{ entity }}
        </span>
      </div>
      <p v-if="graphPath.relationships.length">{{ graphPath.relationships.join(' -> ') }}</p>
      <p v-if="graphPath.sourceChunkIds.length" class="muted-text">
        chunks: {{ graphPath.sourceChunkIds.join(', ') }}
      </p>
      <span v-if="graphPath.confidence !== undefined" class="muted-text">
        confidence: {{ graphPath.confidence.toFixed(3) }}
      </span>
    </article>
  </section>
</template>

<script setup lang="ts">
import type { GraphPath } from '@/features/chat/types'

defineProps<{
  graphPaths: GraphPath[]
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

.graph-path-card {
  display: grid;
  gap: 8px;
  border: 1px solid #dfe5ee;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.graph-path-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.graph-path-card p {
  margin: 0;
  color: #39485f;
  font-size: 13px;
  line-height: 1.6;
}

.path-line {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.path-entity {
  max-width: 100%;
  border: 1px solid #cbd6e4;
  border-radius: 6px;
  background: #f6f8fb;
  padding: 3px 7px;
  color: #223149;
  font-size: 12px;
  overflow-wrap: anywhere;
}
</style>
