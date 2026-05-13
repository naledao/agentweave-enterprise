import type { ApiError } from '@/shared/types/api'

export function isApiError(error: unknown): error is ApiError {
  return (
    typeof error === 'object' &&
    error !== null &&
    'code' in error &&
    'message' in error &&
    'traceId' in error
  )
}

export function getApiErrorDisplay(
  error: unknown,
  fallback = '操作失败',
): { message: string; traceId: string | null } {
  if (!isApiError(error)) {
    return {
      message: fallback,
      traceId: null,
    }
  }

  return {
    message: error.message || fallback,
    traceId: error.traceId || null,
  }
}

export function formatApiError(error: unknown, fallback = '操作失败'): string {
  if (!isApiError(error)) {
    return fallback
  }

  const trace = error.traceId ? ` traceId: ${error.traceId}` : ''
  return `${error.message || fallback}.${trace}`
}
