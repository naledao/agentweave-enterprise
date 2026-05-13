import type { CurrentUser } from '@/features/auth/types'
import { clearToken, readToken, writeToken } from './tokenStorage'

let authToken: string | null = readToken()
let currentUserSnapshot: CurrentUser | null = null

export function getAuthToken(): string | null {
  return authToken
}

export function setAuthToken(token: string): void {
  authToken = token
  writeToken(token)
}

export function getCurrentUserSnapshot(): CurrentUser | null {
  return currentUserSnapshot
}

export function setCurrentUserSnapshot(user: CurrentUser | null): void {
  currentUserSnapshot = user
}

export function clearAuthState(): void {
  authToken = null
  currentUserSnapshot = null
  clearToken()
}
