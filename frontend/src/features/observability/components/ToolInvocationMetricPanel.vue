<template>
  <section class="metric-panel">
    <div class="panel-header">
      <h3>工具调用</h3>
      <span>执行状态、失败率与高频工具</span>
    </div>
    <dl class="panel-metrics">
      <div>
        <dt>总调用</dt>
        <dd>{{ summary?.total ?? 0 }}</dd>
      </div>
      <div>
        <dt>失败率</dt>
        <dd :class="{ danger: (summary?.failureRate ?? 0) > 0 }">{{ formatRate(summary?.failureRate) }}</dd>
      </div>
      <div>
        <dt>拒绝</dt>
        <dd>{{ summary?.denied ?? 0 }}</dd>
      </div>
      <div>
        <dt>平均耗时</dt>
        <dd>{{ formatDuration(summary?.averageDurationMs) }}</dd>
      </div>
    </dl>
    <div class="top-tools">
      <span v-for="tool in topTools" :key="tool.toolCode">{{ tool.toolName }} · {{ tool.count }}</span>
      <span v-if="!topTools.length">暂无工具排行</span>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { ToolInvocationSummary } from '@/features/tools/types'

const props = defineProps<{
  summary: ToolInvocationSummary | null
}>()

const topTools = computed(() => props.summary?.toolCounts.slice(0, 3) ?? [])

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
.panel-metrics dt,
.top-tools span {
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

.top-tools {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 8px;
}

.top-tools span {
  max-width: 100%;
  overflow: hidden;
  border-radius: 999px;
  background: #f1f5f9;
  padding: 4px 8px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
