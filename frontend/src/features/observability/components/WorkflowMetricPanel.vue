<template>
  <section class="metric-panel">
    <div class="panel-header">
      <h3>工作流</h3>
      <span>运行状态与平均耗时</span>
    </div>
    <dl class="panel-metrics">
      <div>
        <dt>运行总数</dt>
        <dd>{{ summary?.total ?? 0 }}</dd>
      </div>
      <div>
        <dt>进行中</dt>
        <dd>{{ summary?.running ?? 0 }}</dd>
      </div>
      <div>
        <dt>失败率</dt>
        <dd :class="{ danger: (summary?.failureRate ?? 0) > 0 }">{{ formatRate(summary?.failureRate) }}</dd>
      </div>
      <div>
        <dt>平均耗时</dt>
        <dd>{{ formatDuration(summary?.averageDurationMs) }}</dd>
      </div>
    </dl>
  </section>
</template>

<script setup lang="ts">
import type { WorkflowSummary } from '@/features/observability/types'

defineProps<{
  summary: WorkflowSummary | null
}>()

function formatDuration(value: number | null | undefined): string {
  return value === null || value === undefined ? '-' : `${Math.round(value)} ms`
}

function formatRate(value: number | null | undefined): string {
  return value === null || value === undefined ? '0.0%' : `${(value * 100).toFixed(1)}%`
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

.panel-metrics dd {
  overflow: hidden;
  margin: 4px 0 0;
  color: #182233;
  font-size: 18px;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.panel-metrics dd.danger {
  color: #c03639;
}
</style>
