import type {
  WorkflowApprovalStatus,
  WorkflowRunStatus,
  WorkflowStepStatus,
  WorkflowStepType,
} from '@/features/workflows/types'

export function formatDateTime(value: string | null | undefined): string {
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

export function formatDuration(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '-'
  }

  if (value < 1000) {
    return `${value} ms`
  }

  return `${(value / 1000).toFixed(1)} s`
}

export function elapsedDuration(startedAt: string | null, finishedAt: string | null): string {
  if (!startedAt || !finishedAt) {
    return '-'
  }

  const elapsed = new Date(finishedAt).getTime() - new Date(startedAt).getTime()
  if (Number.isNaN(elapsed) || elapsed < 0) {
    return '-'
  }

  return formatDuration(elapsed)
}

export function runStatusLabel(status: WorkflowRunStatus): string {
  const map: Record<WorkflowRunStatus, string> = {
    CREATED: '已创建',
    PLANNING: '规划中',
    EXECUTING: '执行中',
    WAITING_APPROVAL: '等待审批',
    REVIEWING: '复核中',
    SUCCEEDED: '成功',
    FAILED: '失败',
    CANCELLED: '已取消',
  }

  return map[status] ?? status
}

export function stepStatusLabel(status: WorkflowStepStatus): string {
  const map: Record<WorkflowStepStatus, string> = {
    PENDING: '等待',
    RUNNING: '执行中',
    WAITING_APPROVAL: '等待审批',
    RETRYING: '重试中',
    SUCCEEDED: '成功',
    FAILED: '失败',
    SKIPPED: '跳过',
  }

  return map[status] ?? status
}

export function approvalStatusLabel(status: WorkflowApprovalStatus): string {
  const map: Record<WorkflowApprovalStatus, string> = {
    PENDING: '待审批',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    CANCELLED: '已取消',
  }

  return map[status] ?? status
}

export function stepTypeLabel(type: WorkflowStepType): string {
  const map: Record<WorkflowStepType, string> = {
    PLANNING: '任务规划',
    RAG_SEARCH: 'Vector RAG',
    GRAPH_RAG_SEARCH: 'GraphRAG',
    TOOL_CALL: '工具调用',
    REVIEW: '结果复核',
    FINAL_ANSWER: '最终答案',
    HUMAN_APPROVAL: '人工审批',
    CHECKPOINT: 'Checkpoint',
    ERROR: '异常处理',
  }

  return map[type] ?? type
}
