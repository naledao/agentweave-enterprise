<template>
  <el-drawer
    :model-value="modelValue"
    destroy-on-close
    size="680px"
    title="步骤详情"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div v-if="step" class="step-detail">
      <section class="detail-section">
        <h3>基础信息</h3>
        <dl class="detail-list">
          <div>
            <dt>步骤</dt>
            <dd>{{ step.stepIndex + 1 }}. {{ stepTypeLabel(step.stepType) }}</dd>
          </div>
          <div>
            <dt>节点</dt>
            <dd>{{ step.nodeName || '-' }}</dd>
          </div>
          <div>
            <dt>状态</dt>
            <dd><WorkflowStepStatusTag :status="step.status" /></dd>
          </div>
          <div>
            <dt>耗时</dt>
            <dd>{{ formatDuration(step.durationMs) }}</dd>
          </div>
          <div>
            <dt>开始时间</dt>
            <dd>{{ formatDateTime(step.startedAt) }}</dd>
          </div>
          <div>
            <dt>结束时间</dt>
            <dd>{{ formatDateTime(step.finishedAt) }}</dd>
          </div>
          <div>
            <dt>重试</dt>
            <dd>{{ step.retryCount }} 次</dd>
          </div>
        </dl>
      </section>

      <section v-if="step.retryReason" class="detail-section">
        <h3>重试原因</h3>
        <pre>{{ step.retryReason }}</pre>
      </section>

      <section class="detail-section">
        <h3>输入摘要</h3>
        <pre>{{ step.inputSummary || '-' }}</pre>
      </section>

      <section class="detail-section">
        <h3>输出摘要</h3>
        <pre>{{ step.outputSummary || '-' }}</pre>
      </section>

      <section v-if="step.errorCode || step.errorMessage" class="detail-section">
        <h3>错误摘要</h3>
        <pre>{{ [step.errorCode, step.errorMessage].filter(Boolean).join('\n') }}</pre>
      </section>

      <section v-if="approval" class="detail-section">
        <h3>审批状态</h3>
        <dl class="detail-list">
          <div>
            <dt>工具</dt>
            <dd>{{ approval.toolCode }}</dd>
          </div>
          <div>
            <dt>状态</dt>
            <dd><WorkflowApprovalStatusTag :status="approval.status" /></dd>
          </div>
          <div>
            <dt>风险</dt>
            <dd>{{ approval.riskLevel }}</dd>
          </div>
          <div>
            <dt>请求时间</dt>
            <dd>{{ formatDateTime(approval.createdAt) }}</dd>
          </div>
          <div>
            <dt>决定时间</dt>
            <dd>{{ formatDateTime(approval.decidedAt) }}</dd>
          </div>
          <div>
            <dt>审批意见</dt>
            <dd>{{ approval.decisionReason || '-' }}</dd>
          </div>
        </dl>
        <pre>{{ approval.requestSummary || '-' }}</pre>
      </section>

      <section class="detail-section">
        <WorkflowCitationList :citations="step.citations ?? []" />
      </section>

      <section class="detail-section">
        <WorkflowGraphPathList :graph-paths="step.graphPaths ?? []" />
      </section>

      <section class="detail-section">
        <WorkflowToolCallList :tool-calls="step.toolCalls ?? []" />
      </section>
    </div>

    <el-empty v-else description="请选择工作流步骤" />
  </el-drawer>
</template>

<script setup lang="ts">
import {
  formatDateTime,
  formatDuration,
  stepTypeLabel,
} from '@/features/workflows/components/workflowFormatters'
import WorkflowApprovalStatusTag from '@/features/workflows/components/WorkflowApprovalStatusTag.vue'
import WorkflowCitationList from '@/features/workflows/components/WorkflowCitationList.vue'
import WorkflowGraphPathList from '@/features/workflows/components/WorkflowGraphPathList.vue'
import WorkflowStepStatusTag from '@/features/workflows/components/WorkflowStepStatusTag.vue'
import WorkflowToolCallList from '@/features/workflows/components/WorkflowToolCallList.vue'
import type { WorkflowApproval, WorkflowStep } from '@/features/workflows/types'

defineProps<{
  modelValue: boolean
  step: WorkflowStep | null
  approval?: WorkflowApproval | null
}>()

defineEmits<{
  'update:modelValue': [value: boolean]
}>()
</script>

<style scoped>
.step-detail {
  display: grid;
  gap: 18px;
}

.detail-section {
  display: grid;
  gap: 12px;
  border-bottom: 1px solid #edf1f5;
  padding-bottom: 18px;
}

.detail-section:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.detail-section h3 {
  margin: 0;
  color: #182233;
  font-size: 15px;
}

.detail-list {
  display: grid;
  gap: 10px;
  margin: 0;
}

.detail-list div {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 12px;
}

.detail-list dt {
  color: #69778d;
  font-size: 13px;
}

.detail-list dd {
  min-width: 0;
  margin: 0;
  color: #263143;
  overflow-wrap: anywhere;
}

pre {
  min-height: 72px;
  max-height: 220px;
  overflow: auto;
  border: 1px solid #dce3ed;
  border-radius: 8px;
  background: #f8fafc;
  color: #263143;
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  line-height: 1.6;
  margin: 0;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
