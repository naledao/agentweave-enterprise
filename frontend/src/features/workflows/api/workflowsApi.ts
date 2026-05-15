import { httpClient } from '@/shared/api/httpClient'
import type {
  ApprovalDecisionPayload,
  WorkflowApproval,
  WorkflowApprovalQuery,
  WorkflowCheckpoint,
  WorkflowRun,
  WorkflowRunPage,
  WorkflowRunQuery,
  WorkflowStep,
} from '@/features/workflows/types'

export const workflowsApi = {
  async listWorkflowRuns(params: WorkflowRunQuery): Promise<WorkflowRunPage> {
    const { data } = await httpClient.get<WorkflowRunPage>('/workflows/runs', { params })
    return data
  },

  async getWorkflowRun(runId: string): Promise<WorkflowRun> {
    const { data } = await httpClient.get<WorkflowRun>(`/workflows/runs/${runId}`)
    return data
  },

  async listWorkflowSteps(runId: string): Promise<WorkflowStep[]> {
    const { data } = await httpClient.get<WorkflowStep[]>(`/workflows/runs/${runId}/steps`)
    return data
  },

  async getLatestWorkflowCheckpoint(runId: string): Promise<WorkflowCheckpoint> {
    const { data } = await httpClient.get<WorkflowCheckpoint>(`/workflows/runs/${runId}/checkpoints/latest`)
    return data
  },

  async listWorkflowApprovals(params: WorkflowApprovalQuery = {}): Promise<WorkflowApproval[]> {
    const { data } = await httpClient.get<WorkflowApproval[]>('/workflows/approvals', { params })
    return data
  },

  async approveWorkflowApproval(
    approvalId: string,
    payload: ApprovalDecisionPayload,
  ): Promise<WorkflowApproval> {
    const { data } = await httpClient.post<WorkflowApproval>(
      `/workflows/approvals/${approvalId}/approve`,
      payload,
    )
    return data
  },

  async rejectWorkflowApproval(
    approvalId: string,
    payload: ApprovalDecisionPayload,
  ): Promise<WorkflowApproval> {
    const { data } = await httpClient.post<WorkflowApproval>(
      `/workflows/approvals/${approvalId}/reject`,
      payload,
    )
    return data
  },
}
