<template>
  <section class="workflow-approval-panel">
    <div class="section-heading">
      <h3>审批任务</h3>
      <span>{{ approvals.length }} 项</span>
    </div>

    <el-empty v-if="approvals.length === 0" description="暂无审批任务" />

    <article
      v-for="approval in approvals"
      v-else
      :key="approval.approvalId"
      class="approval-card"
    >
      <div class="approval-title">
        <div>
          <strong>{{ approval.toolCode }}</strong>
          <span>步骤 {{ approval.stepIndex + 1 }}</span>
        </div>
        <WorkflowApprovalStatusTag :status="approval.status" />
      </div>

      <p>{{ approval.requestSummary || '无请求摘要' }}</p>

      <dl class="approval-meta">
        <div>
          <dt>风险</dt>
          <dd><el-tag effect="plain" :type="riskType(approval.riskLevel)">{{ approval.riskLevel }}</el-tag></dd>
        </div>
        <div>
          <dt>创建时间</dt>
          <dd>{{ formatDateTime(approval.createdAt) }}</dd>
        </div>
        <div v-if="approval.decisionReason">
          <dt>意见</dt>
          <dd>{{ approval.decisionReason }}</dd>
        </div>
      </dl>

      <div v-if="approval.status === 'PENDING'" class="approval-actions">
        <el-input
          v-model="reasons[approval.approvalId]"
          clearable
          maxlength="500"
          placeholder="审批意见"
        />
        <el-button
          type="primary"
          :loading="pendingActionId === approval.approvalId && pendingAction === 'approve'"
          @click="submitApproval(approval, 'approve')"
        >
          通过
        </el-button>
        <el-button
          type="danger"
          :loading="pendingActionId === approval.approvalId && pendingAction === 'reject'"
          @click="submitApproval(approval, 'reject')"
        >
          拒绝
        </el-button>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { reactive } from 'vue'

import { formatDateTime } from '@/features/workflows/components/workflowFormatters'
import WorkflowApprovalStatusTag from '@/features/workflows/components/WorkflowApprovalStatusTag.vue'
import type { ToolRiskLevel, WorkflowApproval } from '@/features/workflows/types'

defineProps<{
  approvals: WorkflowApproval[]
  pendingActionId?: string
  pendingAction?: 'approve' | 'reject' | ''
}>()

const emit = defineEmits<{
  approve: [approval: WorkflowApproval, reason: string]
  reject: [approval: WorkflowApproval, reason: string]
}>()

const reasons = reactive<Record<string, string>>({})

function submitApproval(approval: WorkflowApproval, action: 'approve' | 'reject'): void {
  const reason = reasons[approval.approvalId]?.trim() ?? ''
  if (action === 'approve') {
    emit('approve', approval, reason)
    return
  }

  emit('reject', approval, reason)
}

function riskType(riskLevel: ToolRiskLevel): 'info' | 'warning' | 'danger' {
  const map: Record<ToolRiskLevel, 'info' | 'warning' | 'danger'> = {
    LOW: 'info',
    MEDIUM: 'warning',
    HIGH: 'danger',
  }

  return map[riskLevel]
}
</script>

<style scoped>
.workflow-approval-panel {
  display: grid;
  gap: 14px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.section-heading,
.approval-title,
.approval-actions {
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

.approval-card {
  display: grid;
  gap: 12px;
  border: 1px solid #dfe5ee;
  border-radius: 8px;
  background: #fbfcfe;
  padding: 12px;
}

.approval-title {
  align-items: flex-start;
}

.approval-title div {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.approval-title strong {
  overflow: hidden;
  color: #182233;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-title span {
  color: #69778d;
  font-size: 12px;
}

p {
  margin: 0;
  color: #39485f;
  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.approval-meta {
  display: grid;
  gap: 8px;
  margin: 0;
}

.approval-meta div {
  display: grid;
  min-width: 0;
  grid-template-columns: 70px minmax(0, 1fr);
  gap: 8px;
}

.approval-meta dt,
.approval-meta dd {
  margin: 0;
  font-size: 12px;
}

.approval-meta dt {
  color: #69778d;
}

.approval-meta dd {
  color: #263143;
  overflow-wrap: anywhere;
}

.approval-actions {
  justify-content: flex-start;
}

.approval-actions .el-input {
  max-width: 320px;
}

@media (max-width: 760px) {
  .approval-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .approval-actions .el-input,
  .approval-actions .el-button {
    width: 100%;
    max-width: none;
  }
}
</style>
