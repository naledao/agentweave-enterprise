import { httpClient } from '@/shared/api/httpClient'
import type {
  ToolDefinition,
  ToolInvocationDetail,
  ToolInvocationPage,
  ToolInvocationQuery,
} from '@/features/tools/types'

export const toolsApi = {
  async listDefinitions(): Promise<ToolDefinition[]> {
    const { data } = await httpClient.get<ToolDefinition[]>('/tools')
    return data
  },

  async listInvocations(params: ToolInvocationQuery): Promise<ToolInvocationPage> {
    const { data } = await httpClient.get<ToolInvocationPage>('/tools/invocations', { params })
    return data
  },

  async getInvocation(invocationId: string): Promise<ToolInvocationDetail> {
    const { data } = await httpClient.get<ToolInvocationDetail>(`/tools/invocations/${invocationId}`)
    return data
  },
}
