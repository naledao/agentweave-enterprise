import { httpClient } from '@/shared/api/httpClient'
import type { PageResponse } from '@/shared/types/api'
import type {
  CreateUserRequest,
  ResetUserPasswordRequest,
  UpdateUserProfileRequest,
  UpdateUserRolesRequest,
  UpdateUserStatusRequest,
  UserProfile,
  UserQuery,
} from '@/features/auth/types'

export const usersApi = {
  async queryUsers(params: UserQuery): Promise<PageResponse<UserProfile>> {
    const { data } = await httpClient.get<PageResponse<UserProfile>>('/users', { params })
    return data
  },

  async getUser(userId: string): Promise<UserProfile> {
    const { data } = await httpClient.get<UserProfile>(`/users/${userId}`)
    return data
  },

  async createUser(request: CreateUserRequest): Promise<UserProfile> {
    const { data } = await httpClient.post<UserProfile>('/users', request)
    return data
  },

  async updateUser(userId: string, request: UpdateUserProfileRequest): Promise<UserProfile> {
    const { data } = await httpClient.put<UserProfile>(`/users/${userId}`, request)
    return data
  },

  async resetPassword(userId: string, request: ResetUserPasswordRequest): Promise<UserProfile> {
    const { data } = await httpClient.patch<UserProfile>(`/users/${userId}/password`, request)
    return data
  },

  async updateUserStatus(userId: string, request: UpdateUserStatusRequest): Promise<UserProfile> {
    const { data } = await httpClient.patch<UserProfile>(`/users/${userId}/status`, request)
    return data
  },

  async updateUserRoles(userId: string, request: UpdateUserRolesRequest): Promise<UserProfile> {
    const { data } = await httpClient.patch<UserProfile>(`/users/${userId}/roles`, request)
    return data
  },
}
