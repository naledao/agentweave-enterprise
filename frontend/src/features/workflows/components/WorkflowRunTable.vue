<template>
  <el-table
    v-loading="loading"
    class="workflow-run-table"
    :data="runs"
    height="100%"
    row-key="runId"
    empty-text="暂无工作流运行记录"
    @row-click="openRun"
  >
    <el-table-column label="目标" min-width="300">
      <template #default="{ row }: { row: WorkflowRun }">
        <div class="run-goal">
          <strong>{{ row.goal }}</strong>
          <span v-if="row.errorMessage">{{ row.errorMessage }}</span>
        </div>
      </template>
    </el-table-column>
    <el-table-column label="状态" width="120">
      <template #default="{ row }: { row: WorkflowRun }">
        <WorkflowStatusTag :status="row.status" />
      </template>
    </el-table-column>
    <el-table-column label="发起人" min-width="190">
      <template #default="{ row }: { row: WorkflowRun }">
        <span class="monospace-text">{{ row.userId }}</span>
      </template>
    </el-table-column>
    <el-table-column label="开始时间" min-width="170">
      <template #default="{ row }: { row: WorkflowRun }">
        {{ formatDateTime(row.startedAt ?? row.createdAt) }}
      </template>
    </el-table-column>
    <el-table-column label="结束时间" min-width="170">
      <template #default="{ row }: { row: WorkflowRun }">
        {{ formatDateTime(row.finishedAt) }}
      </template>
    </el-table-column>
    <el-table-column label="耗时" width="110" align="right">
      <template #default="{ row }: { row: WorkflowRun }">
        {{ elapsedDuration(row.startedAt, row.finishedAt) }}
      </template>
    </el-table-column>
    <el-table-column label="traceId" min-width="230">
      <template #default="{ row }: { row: WorkflowRun }">
        <TraceIdText v-if="row.traceId" :trace-id="row.traceId" />
        <span v-else class="muted-text">-</span>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="100" fixed="right">
      <template #default="{ row }: { row: WorkflowRun }">
        <el-button size="small" @click.stop="openRun(row)">详情</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import {
  elapsedDuration,
  formatDateTime,
} from '@/features/workflows/components/workflowFormatters'
import WorkflowStatusTag from '@/features/workflows/components/WorkflowStatusTag.vue'
import type { WorkflowRun } from '@/features/workflows/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

defineProps<{
  runs: WorkflowRun[]
  loading?: boolean
}>()

const emit = defineEmits<{
  open: [run: WorkflowRun]
}>()

function openRun(run: WorkflowRun): void {
  emit('open', run)
}
</script>

<style scoped>
.workflow-run-table {
  width: 100%;
}

.workflow-run-table :deep(.el-table__row) {
  cursor: pointer;
}

.workflow-run-table :deep(.el-table__inner-wrapper) {
  height: 100%;
}

.run-goal {
  display: grid;
  gap: 4px;
}

.run-goal strong,
.run-goal span {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.run-goal strong {
  color: #182233;
}

.run-goal span {
  color: #c84c4c;
  font-size: 12px;
}

.monospace-text {
  display: inline-block;
  max-width: 170px;
  overflow: hidden;
  color: #5b6b84;
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
