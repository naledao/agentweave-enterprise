<template>
  <el-table
    v-loading="loading"
    class="tool-invocation-table"
    :data="invocations"
    height="100%"
    row-key="id"
    empty-text="暂无工具调用记录"
    @row-click="openInvocation"
  >
    <el-table-column label="工具" min-width="180">
      <template #default="{ row }: { row: ToolInvocation }">
        <span class="tool-name">{{ row.toolName }}</span>
        <span class="monospace-text">{{ row.toolCode }}</span>
      </template>
    </el-table-column>
    <el-table-column label="类型" width="120">
      <template #default="{ row }: { row: ToolInvocation }">
        {{ formatToolType(row.toolType) }}
      </template>
    </el-table-column>
    <el-table-column label="调用人" min-width="150">
      <template #default="{ row }: { row: ToolInvocation }">
        <span>{{ row.username || row.userId }}</span>
      </template>
    </el-table-column>
    <el-table-column label="状态" width="100">
      <template #default="{ row }: { row: ToolInvocation }">
        <ToolInvocationStatusTag :status="row.status" />
      </template>
    </el-table-column>
    <el-table-column label="耗时" width="100" align="right">
      <template #default="{ row }: { row: ToolInvocation }">
        {{ formatDuration(row.durationMs) }}
      </template>
    </el-table-column>
    <el-table-column label="创建时间" min-width="170">
      <template #default="{ row }: { row: ToolInvocation }">
        {{ formatDateTime(row.createdAt) }}
      </template>
    </el-table-column>
    <el-table-column label="错误摘要" min-width="220">
      <template #default="{ row }: { row: ToolInvocation }">
        <span class="summary-text">{{ row.errorMessage || '-' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="traceId" min-width="230">
      <template #default="{ row }: { row: ToolInvocation }">
        <TraceIdText v-if="row.traceId" :trace-id="row.traceId" />
        <span v-else class="muted-text">-</span>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="100" fixed="right">
      <template #default="{ row }: { row: ToolInvocation }">
        <el-button size="small" @click.stop="openInvocation(row)">详情</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import ToolInvocationStatusTag from '@/features/tools/components/ToolInvocationStatusTag.vue'
import type { ToolInvocation } from '@/features/tools/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

defineProps<{
  invocations: ToolInvocation[]
  loading?: boolean
}>()

const emit = defineEmits<{
  open: [invocation: ToolInvocation]
}>()

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

function formatDuration(value: number | null): string {
  if (value === null || value === undefined) {
    return '-'
  }

  return `${value} ms`
}

function formatToolType(value: ToolInvocation['toolType']): string {
  const map: Record<ToolInvocation['toolType'], string> = {
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

function openInvocation(row: ToolInvocation): void {
  emit('open', row)
}
</script>

<style scoped>
.tool-invocation-table {
  width: 100%;
}

.tool-invocation-table :deep(.el-table__row) {
  cursor: pointer;
}

.tool-invocation-table :deep(.el-table__inner-wrapper) {
  height: 100%;
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

.tool-name {
  display: block;
  overflow: hidden;
  color: #263143;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #69778d;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
