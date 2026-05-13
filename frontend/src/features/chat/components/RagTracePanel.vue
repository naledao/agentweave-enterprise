<template>
  <section v-if="visible" class="rag-trace-panel">
    <div class="trace-summary">
      <el-tag v-if="retrievalMode" effect="plain" type="info">{{ retrievalMode }}</el-tag>
      <el-button
        v-if="citations.length"
        text
        type="primary"
        @click="$emit('open-citations')"
      >
        引用 {{ citations.length }}
      </el-button>
      <el-button
        v-if="graphPaths.length"
        text
        type="primary"
        @click="$emit('open-graph-paths')"
      >
        图谱路径 {{ graphPaths.length }}
      </el-button>
      <span v-if="showNoCitation" class="muted-text">无引用资料</span>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { RagCitation, RagGraphPath, RagRetrievalMode } from '@/features/chat/types'

const props = defineProps<{
  retrievalMode?: RagRetrievalMode | null
  citations: RagCitation[]
  graphPaths: RagGraphPath[]
  successful?: boolean
}>()

defineEmits<{
  'open-citations': []
  'open-graph-paths': []
}>()

const visible = computed(() =>
  Boolean(props.retrievalMode)
  || props.citations.length > 0
  || props.graphPaths.length > 0
  || Boolean(props.successful),
)
const showNoCitation = computed(() =>
  Boolean(props.successful) && props.citations.length === 0 && props.graphPaths.length === 0,
)
</script>

<style scoped>
.rag-trace-panel {
  margin-top: 12px;
}

.trace-summary {
  display: flex;
  min-height: 32px;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.muted-text {
  font-size: 13px;
}
</style>
