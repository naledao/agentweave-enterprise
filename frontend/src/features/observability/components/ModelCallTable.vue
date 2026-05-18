<template>
  <el-table
    v-loading="loading"
    class="model-call-table"
    :data="calls"
    height="100%"
    row-key="id"
    empty-text="暂无模型调用记录"
  >
    <el-table-column label="状态" width="105">
      <template #default="{ row }: { row: ModelCall }">
        <el-tag :type="statusType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="模型" min-width="180">
      <template #default="{ row }: { row: ModelCall }">
        <span class="main-text">{{ row.modelName || '-' }}</span>
        <span class="sub-text">{{ row.provider || '-' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="场景" min-width="160">
      <template #default="{ row }: { row: ModelCall }">{{ scenarioLabel(row.scenario) }}</template>
    </el-table-column>
    <el-table-column label="Token" width="95" align="right">
      <template #default="{ row }: { row: ModelCall }">{{ row.totalTokens ?? '-' }}</template>
    </el-table-column>
    <el-table-column label="耗时" width="100" align="right">
      <template #default="{ row }: { row: ModelCall }">{{ formatDuration(row.durationMs) }}</template>
    </el-table-column>
    <el-table-column label="创建时间" min-width="170">
      <template #default="{ row }: { row: ModelCall }">{{ formatDateTime(row.createdAt) }}</template>
    </el-table-column>
    <el-table-column label="摘要" min-width="240">
      <template #default="{ row }: { row: ModelCall }">
        <span class="summary-text">{{ row.errorMessage || row.responseSummary || row.promptSummary || '-' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="traceId" min-width="230">
      <template #default="{ row }: { row: ModelCall }">
        <TraceIdText v-if="row.traceId" :trace-id="row.traceId" />
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import type { ModelCall, ModelCallScenario, ModelCallStatus } from '@/features/observability/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

defineProps<{
  calls: ModelCall[]
  loading?: boolean
}>()

function statusType(status: ModelCallStatus): 'success' | 'warning' | 'danger' | 'info' {
  const normalized = status.toUpperCase()
  if (normalized === 'SUCCESS') {
    return 'success'
  }
  if (normalized === 'FAILED' || normalized === 'TIMEOUT') {
    return 'danger'
  }
  if (normalized === 'CANCELLED') {
    return 'warning'
  }
  return 'info'
}

function statusLabel(status: ModelCallStatus): string {
  const map: Record<string, string> = {
    SUCCESS: '成功',
    FAILED: '失败',
    TIMEOUT: '超时',
    CANCELLED: '取消',
  }
  return map[status.toUpperCase()] ?? status
}

function scenarioLabel(scenario: ModelCallScenario): string {
  const map: Record<string, string> = {
    CHAT_SYNC: '同步对话',
    CHAT_STREAM: '流式对话',
    RAG_ANSWER: 'RAG 回答',
    GRAPHRAG_EXTRACTION: 'GraphRAG 抽取',
    WORKFLOW_PLANNER: '工作流规划',
    WORKFLOW_EXECUTOR: '工作流执行',
    WORKFLOW_REVIEWER: '工作流复核',
  }
  return map[scenario] ?? scenario
}

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

  return `${Math.round(value)} ms`
}
</script>

<style scoped>
.model-call-table {
  width: 100%;
}

.main-text,
.sub-text,
.summary-text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main-text {
  color: #263143;
}

.sub-text,
.summary-text {
  color: #69778d;
  font-size: 12px;
}
</style>
