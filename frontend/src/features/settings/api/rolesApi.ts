import { httpClient } from '@/shared/api/httpClient'
import type { PageResponse } from '@/shared/types/api'
import type {
  CreateRoleRequest,
  Permission,
  Role,
  RoleQuery,
  UpdateRolePermissionsRequest,
  UpdateRoleRequest,
  UpdateRoleStatusRequest,
} from '@/features/auth/types'

export const rolesApi = {
  async queryRoles(): Promise<Role[]> {
    const { data } = await httpClient.get<Role[]>('/roles')
    return data
  },

  async queryRolePage(params: RoleQuery): Promise<PageResponse<Role>> {
    const { data } = await httpClient.get<PageResponse<Role>>('/roles', { params })
    return data
  },

  async getRole(roleId: string): Promise<Role> {
    const { data } = await httpClient.get<Role>(`/roles/${roleId}`)
    return data
  },

  async queryPermissions(): Promise<Permission[]> {
    const { data } = await httpClient.get<Permission[]>('/roles/permissions')
    return data
  },

  async createRole(request: CreateRoleRequest): Promise<Role> {
    const { data } = await httpClient.post<Role>('/roles', request)
    return data
  },

  async updateRole(roleId: string, request: UpdateRoleRequest): Promise<Role> {
    const { data } = await httpClient.put<Role>(`/roles/${roleId}`, request)
    return data
  },

  async updateRolePermissions(roleId: string, request: UpdateRolePermissionsRequest): Promise<Role> {
    const { data } = await httpClient.patch<Role>(`/roles/${roleId}/permissions`, request)
    return data
  },

  async updateRoleStatus(roleId: string, request: UpdateRoleStatusRequest): Promise<Role> {
    const { data } = await httpClient.patch<Role>(`/roles/${roleId}/status`, request)
    return data
  },
}
