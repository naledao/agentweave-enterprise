<template>
  <section class="metric-panel">
    <div class="panel-header">
      <h3>GraphRAG</h3>
      <span>图谱构建与路径检索</span>
    </div>
    <dl class="panel-metrics">
      <div>
        <dt>构建日志</dt>
        <dd>{{ summary?.indexLogCount ?? 0 }}</dd>
      </div>
      <div>
        <dt>检索日志</dt>
        <dd>{{ summary?.retrievalLogCount ?? 0 }}</dd>
      </div>
      <div>
        <dt>最近路径</dt>
        <dd>{{ summary?.latestRetrievalLog?.filteredPathCount ?? 0 }}</dd>
      </div>
      <div>
        <dt>最近耗时</dt>
        <dd>{{ formatDuration(summary?.latestRetrievalLog?.durationMs) }}</dd>
      </div>
    </dl>
  </section>
</template>

<script setup lang="ts">
import type { GraphRagSummary } from '@/features/observability/types'

defineProps<{
  summary: GraphRagSummary | null
}>()

function formatDuration(value: number | null | undefined): string {
  return value === null || value === undefined ? '-' : `${Math.round(value)} ms`
}
</script>

<style scoped>
.metric-panel {
  display: grid;
  gap: 12px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.panel-header {
  display: grid;
  gap: 4px;
}

.panel-header h3 {
  margin: 0;
  color: #182233;
  font-size: 15px;
}

.panel-header span,
.panel-metrics dt {
  color: #69778d;
  font-size: 12px;
}

.panel-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.panel-metrics div {
  min-width: 0;
}

.panel-metrics dt {
  margin-bottom: 4px;
}

.panel-metrics dd {
  overflow: hidden;
  margin: 0;
  color: #182233;
  font-size: 18px;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
