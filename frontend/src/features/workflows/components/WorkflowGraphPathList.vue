<template>
  <section class="summary-list">
    <h4>图谱路径</h4>
    <div v-if="graphPaths.length === 0" class="list-empty">暂无图谱路径</div>
    <article v-for="(path, index) in graphPaths" v-else :key="path.pathId ?? index" class="summary-card">
      <div class="card-title">
        <strong>{{ path.pathId || 'Graph path' }}</strong>
        <div class="path-tags">
          <el-tag effect="plain" type="info">{{ path.depth }} hop</el-tag>
          <el-tag v-if="path.confidence !== null && path.confidence !== undefined" effect="plain" type="success">
            {{ path.confidence.toFixed(3) }}
          </el-tag>
        </div>
      </div>

      <div v-if="path.entities.length" class="path-line">
        <template v-for="(entity, entityIndex) in path.entities" :key="`${entity}-${entityIndex}`">
          <span class="path-entity">{{ entity }}</span>
          <span v-if="entityIndex < path.relationships.length" class="path-relationship">
            {{ path.relationships[entityIndex] }}
          </span>
        </template>
      </div>

      <p v-if="path.sourceChunkIds.length">chunks: {{ path.sourceChunkIds.join(', ') }}</p>
    </article>
  </section>
</template>

<script setup lang="ts">
import type { WorkflowGraphPath } from '@/features/workflows/types'

defineProps<{
  graphPaths: WorkflowGraphPath[]
}>()
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
  gap: 10px;
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

.path-tags {
  display: inline-flex;
  flex: none;
  flex-wrap: wrap;
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

p {
  margin: 0;
  color: #69778d;
  font-size: 12px;
  overflow-wrap: anywhere;
}
</style>
