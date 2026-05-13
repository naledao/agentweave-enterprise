<template>
  <section class="panel-section">
    <h3>工作流步骤</h3>
    <div v-if="steps.length === 0" class="panel-empty">暂无工作流步骤</div>
    <el-timeline v-else>
      <el-timeline-item
        v-for="step in steps"
        :key="step.workflowRunId"
        :type="timelineType(step.status)"
        :timestamp="statusLabel(step.status)"
      >
        <strong>{{ step.stepName }}</strong>
        <TraceIdText v-if="step.traceId" class="step-trace" :trace-id="step.traceId" />
      </el-timeline-item>
    </el-timeline>
  </section>
</template>

<script setup lang="ts">
import type { WorkflowStep, WorkflowStepStatus } from '@/features/chat/types'
import TraceIdText from '@/shared/components/TraceIdText.vue'

defineProps<{
  steps: WorkflowStep[]
}>()

function timelineType(status: WorkflowStepStatus): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<WorkflowStepStatus, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    PENDING: 'info',
    RUNNING: 'primary',
    SUCCEEDED: 'success',
    FAILED: 'danger',
    SKIPPED: 'warning',
  }

  return map[status]
}

function statusLabel(status: WorkflowStepStatus): string {
  const map: Record<WorkflowStepStatus, string> = {
    PENDING: '等待',
    RUNNING: '执行中',
    SUCCEEDED: '成功',
    FAILED: '失败',
    SKIPPED: '跳过',
  }

  return map[status]
}
</script>

<style scoped>
.panel-section {
  display: grid;
  gap: 10px;
}

.panel-section h3 {
  margin: 0;
  color: #182233;
  font-size: 15px;
}

.panel-empty {
  color: #69778d;
  font-size: 13px;
}

:deep(.el-timeline) {
  padding-left: 4px;
}

p {
  margin: 6px 0 0;
  color: #39485f;
  font-size: 13px;
  line-height: 1.6;
}

.step-trace {
  margin-top: 6px;
}
</style>
