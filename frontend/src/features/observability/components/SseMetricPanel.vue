<template>
  <section class="metric-panel">
    <div class="panel-header">
      <h3>SSE 连接</h3>
      <span>连接存活、首 token 与异常结束</span>
    </div>
    <dl class="panel-metrics">
      <div>
        <dt>活动连接</dt>
        <dd>{{ round(summary?.activeConnections) }}</dd>
      </div>
      <div>
        <dt>完成连接</dt>
        <dd>{{ round(summary?.completedConnections) }}</dd>
      </div>
      <div>
        <dt>连接时长</dt>
        <dd>{{ formatDuration(summary?.averageConnectionDurationMs) }}</dd>
      </div>
      <div>
        <dt>首 token</dt>
        <dd>{{ formatDuration(summary?.averageFirstTokenDurationMs) }}</dd>
      </div>
    </dl>
  </section>
</template>

<script setup lang="ts">
import type { SseSummary } from '@/features/observability/types'

defineProps<{
  summary: SseSummary | null
}>()

function round(value: number | null | undefined): number {
  return Math.round(value ?? 0)
}

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

.panel-metrics dd {
  overflow: hidden;
  margin: 4px 0 0;
  color: #182233;
  font-size: 18px;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
