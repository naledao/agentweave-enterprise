<template>
  <section class="workflow-run-detail-view">
    <PageHeader
      :title="run?.goal || '工作流详情'"
      description="查看完整执行步骤、审批状态、最终结果和 traceId。"
      eyebrow="Workflow Detail"
    >
      <template #actions>
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="pageError.message"
      class="page-error"
      :title="pageError.message"
      type="error"
      :closable="false"
      show-icon
    >
      <TraceIdText v-if="pageError.traceId" :trace-id="pageError.traceId" />
    </el-alert>

    <div v-loading="runQuery.isFetching.value" class="detail-shell">
      <template v-if="run">
        <section class="page-surface run-summary">
          <div class="summary-main">
            <WorkflowStatusTag :status="run.status" />
            <TraceIdText v-if="run.traceId" :trace-id="run.traceId" />
          </div>
          <dl class="summary-meta">
            <div>
              <dt>发起人</dt>
              <dd>{{ run.userId }}</dd>
            </div>
            <div>
              <dt>开始时间</dt>
              <dd>{{ formatDateTime(run.startedAt ?? run.createdAt) }}</dd>
            </div>
            <div>
              <dt>结束时间</dt>
              <dd>{{ formatDateTime(run.finishedAt) }}</dd>
            </div>
            <div>
              <dt>耗时</dt>
              <dd>{{ elapsedDuration(run.startedAt, run.finishedAt) }}</dd>
            </div>
          </dl>
        </section>

        <section v-if="checkpoint" class="page-surface checkpoint-summary">
          <strong>最新 Checkpoint</strong>
          <span>{{ checkpoint.nodeName || '-' }} · step {{ checkpoint.stepIndex }} · v{{ checkpoint.stateVersion }}</span>
          <el-tag :type="checkpoint.recoverable ? 'success' : 'info'" effect="plain">
            {{ checkpoint.recoverable ? '可恢复' : '不可恢复' }}
          </el-tag>
        </section>

        <div class="detail-grid">
          <div class="timeline-column page-surface">
            <WorkflowTimeline :steps="steps" @open-step="openStep" />
          </div>

          <div class="side-column">
            <WorkflowResultPanel :run="run" />
            <WorkflowApprovalPanel
              :approvals="runApprovals"
              :pending-action="approvalAction"
              :pending-action-id="approvalActionId"
              @approve="approveApproval"
              @reject="rejectApproval"
            />
          </div>
        </div>
      </template>

      <el-empty v-else-if="!runQuery.isFetching.value && !pageError.message" description="未找到工作流运行" />
    </div>

    <WorkflowStepDetailDrawer
      v-model="stepDrawerVisible"
      :approval="selectedStepApproval"
      :step="selectedStep"
    />
  </section>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ElMessage } from 'element-plus'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { workflowsApi } from '@/features/workflows/api/workflowsApi'
import {
  elapsedDuration,
  formatDateTime,
} from '@/features/workflows/components/workflowFormatters'
import WorkflowApprovalPanel from '@/features/workflows/components/WorkflowApprovalPanel.vue'
import WorkflowResultPanel from '@/features/workflows/components/WorkflowResultPanel.vue'
import WorkflowStatusTag from '@/features/workflows/components/WorkflowStatusTag.vue'
import WorkflowStepDetailDrawer from '@/features/workflows/components/WorkflowStepDetailDrawer.vue'
import WorkflowTimeline from '@/features/workflows/components/WorkflowTimeline.vue'
import type { WorkflowApproval, WorkflowStep } from '@/features/workflows/types'
import PageHeader from '@/shared/components/PageHeader.vue'
import TraceIdText from '@/shared/components/TraceIdText.vue'
import { formatApiError, getApiErrorDisplay } from '@/shared/utils/apiError'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()

const runId = computed(() => String(route.params.runId ?? ''))
const selectedStep = ref<WorkflowStep | null>(null)
const stepDrawerVisible = ref(false)
const approvalActionId = ref('')
const approvalAction = ref<'approve' | 'reject' | ''>('')

const runQuery = useQuery({
  queryKey: computed(() => ['workflow-run', runId.value]),
  queryFn: () => workflowsApi.getWorkflowRun(runId.value),
  enabled: computed(() => Boolean(runId.value)),
})

const stepsQuery = useQuery({
  queryKey: computed(() => ['workflow-steps', runId.value]),
  queryFn: () => workflowsApi.listWorkflowSteps(runId.value),
  enabled: computed(() => Boolean(runId.value)),
})

const approvalsQuery = useQuery({
  queryKey: computed(() => ['workflow-approvals', { status: undefined }]),
  queryFn: () => workflowsApi.listWorkflowApprovals(),
})

const checkpointQuery = useQuery({
  queryKey: computed(() => ['workflow-checkpoint-latest', runId.value]),
  queryFn: () => workflowsApi.getLatestWorkflowCheckpoint(runId.value),
  enabled: computed(() => Boolean(runId.value)),
  retry: false,
})

const approvalMutation = useMutation({
  mutationFn: (payload: { approval: WorkflowApproval; action: 'approve' | 'reject'; reason: string }) => {
    approvalActionId.value = payload.approval.approvalId
    approvalAction.value = payload.action
    const body = payload.reason ? { reason: payload.reason } : {}
    return payload.action === 'approve'
      ? workflowsApi.approveWorkflowApproval(payload.approval.approvalId, body)
      : workflowsApi.rejectWorkflowApproval(payload.approval.approvalId, body)
  },
  async onSuccess(_, payload) {
    ElMessage.success(payload.action === 'approve' ? '审批已通过' : '审批已拒绝')
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['workflow-run', runId.value] }),
      queryClient.invalidateQueries({ queryKey: ['workflow-steps', runId.value] }),
      queryClient.invalidateQueries({ queryKey: ['workflow-approvals'] }),
      queryClient.invalidateQueries({ queryKey: ['workflow-runs'] }),
    ])
  },
  onError(error) {
    ElMessage.error(formatApiError(error, '审批操作失败'))
  },
  onSettled() {
    approvalActionId.value = ''
    approvalAction.value = ''
  },
})

const run = computed(() => runQuery.data.value ?? null)
const steps = computed(() => stepsQuery.data.value ?? [])
const checkpoint = computed(() => checkpointQuery.data.value ?? null)
const runApprovals = computed(() =>
  (approvalsQuery.data.value ?? []).filter((approval) => approval.runId === runId.value),
)
const selectedStepApproval = computed(() => {
  if (!selectedStep.value) {
    return null
  }

  return runApprovals.value.find((approval) => approval.stepId === selectedStep.value?.stepId) ?? null
})
const pageError = computed(() => {
  const error = runQuery.error.value ?? stepsQuery.error.value ?? approvalsQuery.error.value
  if (!error) {
    return { message: '', traceId: null }
  }

  return getApiErrorDisplay(error, '工作流详情加载失败')
})

watch(stepDrawerVisible, (visible) => {
  if (!visible) {
    selectedStep.value = null
  }
})

function openStep(step: WorkflowStep): void {
  selectedStep.value = step
  stepDrawerVisible.value = true
}

function approveApproval(approval: WorkflowApproval, reason: string): void {
  approvalMutation.mutate({ approval, action: 'approve', reason })
}

function rejectApproval(approval: WorkflowApproval, reason: string): void {
  approvalMutation.mutate({ approval, action: 'reject', reason })
}

async function goBack(): Promise<void> {
  await router.push({ name: 'WorkflowRuns' })
}
</script>

<style scoped>
.workflow-run-detail-view {
  display: flex;
  min-height: 100%;
  flex-direction: column;
}

.page-error {
  flex: none;
  margin-bottom: 16px;
}

.detail-shell {
  display: grid;
  gap: 16px;
  min-height: 240px;
}

.run-summary {
  display: grid;
  gap: 14px;
  padding: 16px;
}

.summary-main {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.summary-meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
}

.summary-meta div {
  min-width: 0;
}

.summary-meta dt,
.summary-meta dd {
  margin: 0;
  font-size: 13px;
}

.summary-meta dt {
  color: #69778d;
}

.summary-meta dd {
  overflow: hidden;
  color: #263143;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.checkpoint-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
}

.checkpoint-summary strong {
  color: #182233;
}

.checkpoint-summary span {
  color: #69778d;
  font-size: 13px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 16px;
  align-items: start;
}

.timeline-column {
  padding: 16px;
}

.side-column {
  display: grid;
  gap: 16px;
}

@media (max-width: 1080px) {
  .detail-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .summary-meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .summary-meta {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
