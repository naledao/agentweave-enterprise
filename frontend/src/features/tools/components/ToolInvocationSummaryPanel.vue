<template>
  <section class="tool-invocation-summary-panel">
    <div class="summary-metrics">
      <div class="metric-item">
        <span>总调用</span>
        <strong>{{ summary?.total ?? 0 }}</strong>
      </div>
      <div class="metric-item">
        <span>成功</span>
        <strong>{{ summary?.success ?? 0 }}</strong>
      </div>
      <div class="metric-item danger">
        <span>失败率</span>
        <strong>{{ formatRate(summary?.failureRate) }}</strong>
      </div>
      <div class="metric-item warning">
        <span>拒绝率</span>
        <strong>{{ formatRate(summary?.deniedRate) }}</strong>
      </div>
      <div class="metric-item">
        <span>平均耗时</span>
        <strong>{{ formatDuration(summary?.averageDurationMs) }}</strong>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="topTools"
      border
      size="small"
      empty-text="暂无聚合数据"
    >
      <el-table-column label="工具" min-width="180">
        <template #default="{ row }: { row: ToolInvocationToolCount }">
          <span class="tool-name">{{ row.toolName }}</span>
          <span class="monospace-text">{{ row.toolCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="120">
        <template #default="{ row }: { row: ToolInvocationToolCount }">
          {{ formatToolType(row.toolType) }}
        </template>
      </el-table-column>
      <el-table-column prop="count" label="调用" width="80" align="right" />
      <el-table-column prop="failed" label="失败" width="80" align="right" />
      <el-table-column prop="denied" label="拒绝" width="80" align="right" />
      <el-table-column prop="timeout" label="超时" width="80" align="right" />
      <el-table-column label="平均耗时" width="110" align="right">
        <template #default="{ row }: { row: ToolInvocationToolCount }">
          {{ formatDuration(row.averageDurationMs) }}
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type {
  ToolInvocationSummary,
  ToolInvocationToolCount,
  ToolType,
} from '@/features/tools/types'

const props = defineProps<{
  summary: ToolInvocationSummary | null
  loading?: boolean
}>()

const topTools = computed(() => props.summary?.toolCounts.slice(0, 5) ?? [])

function formatRate(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '0.0%'
  }
  return `${(value * 100).toFixed(1)}%`
}

function formatDuration(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '-'
  }
  return `${Math.round(value)} ms`
}

function formatToolType(value: ToolType): string {
  const map: Record<ToolType, string> = {
    BUSINESS_QUERY: '业务查询',
    LOG_SEARCH: '日志检索',
    DATABASE_READ: '数据库只读',
    ENDPOINT_STATUS: '接口状态',
    NOTIFICATION: '消息通知',
    MCP_RESOURCE: 'MCP 资源',
    SCRIPT: '脚本',
    UNKNOWN: '未知',
  }
  return map[value] ?? value
}
</script>

<style scoped>
.tool-invocation-summary-panel {
  display: grid;
  gap: 12px;
  border: 1px solid #dce3ed;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
  margin-bottom: 16px;
}

.summary-metrics {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.metric-item {
  display: grid;
  gap: 6px;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  background: #f8fafc;
  padding: 10px 12px;
}

.metric-item span {
  color: #69778d;
  font-size: 12px;
}

.metric-item strong {
  color: #182233;
  font-size: 20px;
  line-height: 1;
}

.metric-item.danger strong {
  color: #c03639;
}

.metric-item.warning strong {
  color: #a15c09;
}

.tool-name {
  display: block;
  overflow: hidden;
  color: #263143;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.monospace-text {
  display: inline-block;
  max-width: 160px;
  overflow: hidden;
  color: #48566b;
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 980px) {
  .summary-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
