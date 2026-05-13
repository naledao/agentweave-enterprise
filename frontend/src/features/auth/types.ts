import type { PageQuery } from '@/shared/types/api'

export type UserStatus = 'ACTIVE' | 'DISABLED' | 'LOCKED'
export type RoleStatus = 'ACTIVE' | 'DISABLED'
export type PermissionType = 'API' | 'MENU' | 'BUTTON' | 'TOOL'
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'

export interface LoginRequest {
  username: string
  password: string
}

export interface UserProfile {
  id: string
  username: string
  displayName: string
  email: string | null
  status: UserStatus
  roles: string[]
  permissions: string[]
  lastLoginAt: string | null
}

export type CurrentUser = UserProfile

export interface LoginResponse {
  accessToken: string
  tokenType: 'Bearer' | string
  expiresIn: number
  user: CurrentUser
}

export interface UserQuery extends PageQuery {
  keyword?: string
  status?: UserStatus | ''
}

export type RoleQuery = PageQuery

export interface CreateUserRequest {
  username: string
  displayName: string
  password: string
  email?: string
  roleIds: string[]
}

export interface UpdateUserProfileRequest {
  displayName: string
  email?: string
}

export interface ResetUserPasswordRequest {
  password: string
}

export interface UpdateUserRolesRequest {
  roleIds: string[]
}

export interface UpdateUserStatusRequest {
  status: UserStatus
}

export interface Permission {
  id: string
  code: string
  name: string
  type: PermissionType
  description?: string | null
  riskLevel?: RiskLevel
}

export interface Role {
  id: string
  code: string
  name: string
  description?: string | null
  status: RoleStatus
  permissions: Permission[]
}

export interface CreateRoleRequest {
  code: string
  name: string
  description?: string
  permissionIds: string[]
}

export interface UpdateRoleRequest {
  name: string
  description?: string
}

export interface UpdateRolePermissionsRequest {
  permissionIds: string[]
}

export interface UpdateRoleStatusRequest {
  status: RoleStatus
}
