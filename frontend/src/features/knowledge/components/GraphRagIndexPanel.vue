<template>
  <div class="graph-rag-panel">
    <div class="panel-header">
      <div>
        <p class="panel-label">GraphRAG</p>
        <h3>图谱构建</h3>
        <p class="panel-description">{{ statusMeta.description }}</p>
      </div>
      <div class="panel-actions">
        <el-tag :type="statusMeta.type" effect="plain">
          {{ statusMeta.label }}
        </el-tag>
        <el-button
          type="primary"
          size="small"
          :loading="rebuilding"
          :disabled="rebuildDisabled || graphRag.status === 'processing'"
          @click="emit('rebuild')"
        >
          <el-icon><Refresh /></el-icon>
          重建图谱索引
        </el-button>
      </div>
    </div>

    <dl class="metrics-grid">
      <div>
        <dt>实体数量</dt>
        <dd>{{ graphRag.entityCount }}</dd>
      </div>
      <div>
        <dt>关系数量</dt>
        <dd>{{ graphRag.relationshipCount }}</dd>
      </div>
      <div>
        <dt>关联 Chunk</dt>
        <dd>{{ graphRag.chunkCount }}</dd>
      </div>
      <div>
        <dt>完成时间</dt>
        <dd>{{ formatDateTime(graphRag.indexedAt) }}</dd>
      </div>
    </dl>

    <el-alert
      v-if="graphRag.errorMessage"
      class="status-error"
      :title="graphRag.errorMessage"
      type="error"
      :closable="false"
      show-icon
    />

    <TraceIdText v-if="graphRag.traceId" :trace-id="graphRag.traceId" />
  </div>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue'
import { computed } from 'vue'

import { fallbackGraphRagStatusMeta, graphRagIndexStatusMeta } from '@/features/knowledge/components/graphRagStatus'
import type { GraphRagIndexSummaryResponse } from '@/features/knowledge/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

const props = withDefaults(defineProps<{
  graphRag: GraphRagIndexSummaryResponse
  rebuilding?: boolean
  rebuildDisabled?: boolean
}>(), {
  rebuilding: false,
  rebuildDisabled: false,
})

const emit = defineEmits<{
  rebuild: []
}>()

const statusMeta = computed(() => {
  return graphRagIndexStatusMeta[props.graphRag.status] ?? fallbackGraphRagStatusMeta(props.graphRag.status)
})

function formatDateTime(value: string | null): string {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
</script>

<style scoped>
.graph-rag-panel {
  display: grid;
  gap: 16px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-label {
  margin: 0 0 4px;
  color: #69778d;
  font-size: 12px;
}

h3 {
  margin: 0;
  color: #182233;
  font-size: 15px;
}

.panel-description {
  margin: 6px 0 0;
  color: #69778d;
  font-size: 13px;
  line-height: 1.5;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
  margin: 0;
}

.metrics-grid div {
  min-width: 0;
}

.metrics-grid dt {
  color: #69778d;
  font-size: 12px;
}

.metrics-grid dd {
  overflow-wrap: anywhere;
  margin: 4px 0 0;
  color: #263143;
  font-weight: 600;
}

.status-error {
  margin-top: 0;
}

:deep(.trace-id-text) {
  margin-top: 0;
}

@media (max-width: 640px) {
  .panel-header {
    display: block;
  }

  .panel-actions {
    justify-content: flex-start;
    margin-top: 12px;
  }

  .metrics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
