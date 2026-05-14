export type ToolRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'

export interface ToolDefinition {
  id: string
  code: string
  name: string
  description: string | null
  permissionCode: string
  riskLevel: ToolRiskLevel
  enabled: boolean
  available: boolean
  inputSchema: string | null
  outputSchema: string | null
  createdAt: string
  updatedAt: string
}
