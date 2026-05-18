import { httpClient } from '@/shared/api/httpClient'
import type {
  AuditLogPage,
  AuditLogQuery,
  GraphRagIndexLogPage,
  GraphRagIndexLogQuery,
  GraphRagRetrievalLogPage,
  GraphRagRetrievalLogQuery,
  GraphRagSummary,
  ModelCallPage,
  ModelCallQuery,
  ObservabilitySummary,
  RagRetrievalLogPage,
  RagRetrievalLogQuery,
} from '@/features/observability/types'

export const observabilityApi = {
  async getSummary(): Promise<ObservabilitySummary> {
    const { data } = await httpClient.get<ObservabilitySummary>('/observability/summary')
    return data
  },

  async getGraphRagSummary(): Promise<GraphRagSummary> {
    const { data } = await httpClient.get<GraphRagSummary>('/observability/graphrag')
    return data
  },

  async listGraphRagIndexLogs(params: GraphRagIndexLogQuery): Promise<GraphRagIndexLogPage> {
    const { data } = await httpClient.get<GraphRagIndexLogPage>('/observability/graphrag/index-logs', { params })
    return data
  },

  async listGraphRagRetrievalLogs(params: GraphRagRetrievalLogQuery): Promise<GraphRagRetrievalLogPage> {
    const { data } = await httpClient.get<GraphRagRetrievalLogPage>('/observability/graphrag/retrieval-logs', { params })
    return data
  },

  async listModelCalls(params: ModelCallQuery): Promise<ModelCallPage> {
    const { data } = await httpClient.get<ModelCallPage>('/observability/model-calls', { params })
    return data
  },

  async listAuditLogs(params: AuditLogQuery): Promise<AuditLogPage> {
    const { data } = await httpClient.get<AuditLogPage>('/observability/audit-logs', { params })
    return data
  },

  async listRagRetrievalLogs(params: RagRetrievalLogQuery): Promise<RagRetrievalLogPage> {
    const { data } = await httpClient.get<RagRetrievalLogPage>('/observability/rag-retrievals', { params })
    return data
  },
}
