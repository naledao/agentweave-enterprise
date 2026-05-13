import { defineStore } from 'pinia'

import { authApi } from '@/features/auth/api/authApi'
import type { CurrentUser, LoginRequest } from '@/features/auth/types'
import {
  clearAuthState,
  getAuthToken,
  getCurrentUserSnapshot,
  setAuthToken,
  setCurrentUserSnapshot,
} from './authSession'

interface AuthState {
  token: string | null
  currentUser: CurrentUser | null
  initialized: boolean
  loading: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: getAuthToken(),
    currentUser: getCurrentUserSnapshot(),
    initialized: false,
    loading: false,
  }),

  getters: {
    permissions: (state): string[] => state.currentUser?.permissions ?? [],
    isAuthenticated: (state): boolean => Boolean(state.token && state.currentUser),
  },

  actions: {
    async login(request: LoginRequest): Promise<void> {
      this.loading = true
      try {
        const response = await authApi.login(request)
        this.token = response.accessToken
        this.currentUser = response.user
        this.initialized = true
        setAuthToken(response.accessToken)
        setCurrentUserSnapshot(response.user)
      } finally {
        this.loading = false
      }
    },

    async loadCurrentUser(): Promise<void> {
      if (!this.token) {
        this.currentUser = null
        return
      }

      this.loading = true
      try {
        const user = await authApi.getCurrentUser()
        this.currentUser = user
        setCurrentUserSnapshot(user)
      } finally {
        this.loading = false
      }
    },

    async logout(): Promise<void> {
      try {
        if (this.token) {
          await authApi.logout()
        }
      } finally {
        this.clearSession()
      }
    },

    async restoreSession(): Promise<void> {
      if (this.initialized) {
        return
      }

      this.token = getAuthToken()
      this.currentUser = getCurrentUserSnapshot()
      this.initialized = true

      if (this.token && !this.currentUser) {
        await this.loadCurrentUser()
      }
    },

    clearSession(): void {
      clearAuthState()
      this.token = null
      this.currentUser = null
      this.initialized = true
    },

    hasPermission(permission: string): boolean {
      if (this.hasRole('ADMIN')) {
        return true
      }

      return this.permissions.includes(permission)
    },

    hasAnyPermission(permissions: string[]): boolean {
      if (permissions.length === 0) {
        return true
      }

      return permissions.some((permission) => this.hasPermission(permission))
    },

    hasRole(role: string): boolean {
      return this.currentUser?.roles.includes(role) ?? false
    },
  },
})
