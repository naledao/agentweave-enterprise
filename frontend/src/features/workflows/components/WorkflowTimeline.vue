<template>
  <section class="workflow-timeline">
    <div class="section-heading">
      <h3>执行步骤</h3>
      <span>{{ steps.length }} 个步骤</span>
    </div>

    <el-empty v-if="steps.length === 0" description="暂无工作流步骤" />

    <el-timeline v-else>
      <el-timeline-item
        v-for="step in steps"
        :key="step.stepId"
        :type="timelineType(step.status)"
        :timestamp="formatDateTime(step.startedAt ?? step.finishedAt)"
      >
        <button class="step-card" type="button" @click="$emit('open-step', step)">
          <div class="step-card-title">
            <strong>{{ step.stepIndex + 1 }}. {{ stepTypeLabel(step.stepType) }}</strong>
            <WorkflowStepStatusTag :status="step.status" />
          </div>
          <p>{{ step.nodeName || step.stepType }}</p>
          <div class="step-card-meta">
            <span>{{ formatDuration(step.durationMs) }}</span>
            <span v-if="step.retryCount > 0">重试 {{ step.retryCount }} 次</span>
            <span v-if="step.errorMessage" class="error-text">{{ step.errorMessage }}</span>
          </div>
        </button>
      </el-timeline-item>
    </el-timeline>
  </section>
</template>

<script setup lang="ts">
import {
  formatDateTime,
  formatDuration,
  stepTypeLabel,
} from '@/features/workflows/components/workflowFormatters'
import WorkflowStepStatusTag from '@/features/workflows/components/WorkflowStepStatusTag.vue'
import type { WorkflowStep, WorkflowStepStatus } from '@/features/workflows/types'

defineProps<{
  steps: WorkflowStep[]
}>()

defineEmits<{
  'open-step': [step: WorkflowStep]
}>()

function timelineType(status: WorkflowStepStatus): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<WorkflowStepStatus, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    PENDING: 'info',
    RUNNING: 'primary',
    WAITING_APPROVAL: 'warning',
    RETRYING: 'warning',
    SUCCEEDED: 'success',
    FAILED: 'danger',
    SKIPPED: 'warning',
  }

  return map[status]
}
</script>

<style scoped>
.workflow-timeline {
  display: grid;
  gap: 14px;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-heading h3 {
  margin: 0;
  color: #182233;
  font-size: 16px;
}

.section-heading span {
  color: #69778d;
  font-size: 13px;
}

:deep(.el-timeline) {
  padding-left: 4px;
}

.step-card {
  display: grid;
  width: 100%;
  gap: 8px;
  border: 1px solid #dfe5ee;
  border-radius: 8px;
  background: #fff;
  color: inherit;
  cursor: pointer;
  padding: 12px;
  text-align: left;
}

.step-card:hover {
  border-color: #9fb7e8;
  background: #f8fbff;
}

.step-card-title,
.step-card-meta {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.step-card-title strong {
  overflow: hidden;
  color: #182233;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-card p {
  margin: 0;
  color: #5b6b84;
  font-size: 13px;
}

.step-card-meta {
  justify-content: flex-start;
  color: #69778d;
  font-size: 12px;
}

.error-text {
  color: #c84c4c;
}
</style>
