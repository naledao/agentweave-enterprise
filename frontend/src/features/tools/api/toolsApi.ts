import { httpClient } from '@/shared/api/httpClient'
import type { ToolDefinition } from '@/features/tools/types'

export const toolsApi = {
  async listDefinitions(): Promise<ToolDefinition[]> {
    const { data } = await httpClient.get<ToolDefinition[]>('/tools')
    return data
  },
}
