<template>
  <section v-if="shouldShow" class="panel-section">
    <div class="panel-title-row">
      <h3>图谱路径</h3>
      <el-tag v-if="graphPaths.length" effect="plain" type="info">{{ graphPaths.length }}</el-tag>
    </div>
    <div v-if="permissionDenied" class="panel-empty">
      当前权限不可查看部分图谱路径。
    </div>
    <div v-else-if="graphPaths.length === 0" class="panel-empty">
      暂无图谱路径
    </div>
    <GraphPathCard
      v-for="graphPath in graphPaths"
      :key="graphPathKey(graphPath)"
      :graph-path="graphPath"
    />
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import GraphPathCard from '@/features/chat/components/GraphPathCard.vue'
import type { RagGraphPath } from '@/features/chat/types'

const props = withDefaults(defineProps<{
  graphPaths: RagGraphPath[]
  permissionDenied?: boolean
  hideWhenEmpty?: boolean
}>(), {
  permissionDenied: false,
  hideWhenEmpty: false,
})

const shouldShow = computed(() => props.permissionDenied || props.graphPaths.length > 0 || !props.hideWhenEmpty)

function graphPathKey(graphPath: RagGraphPath): string {
  return graphPath.pathId ?? `${graphPath.entities.join('>')}:${graphPath.relationships.join('>')}`
}
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
