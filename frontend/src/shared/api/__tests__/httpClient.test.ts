import { describe, expect, it, vi } from 'vitest'

import { clearAuthState, setAuthToken } from '@/features/auth/store/authSession'
import { httpClient } from '@/shared/api/httpClient'

describe('httpClient', () => {
  it('adds Authorization header when token exists', async () => {
    setAuthToken('token-1')
    const requestHandler = httpClient.interceptors.request.handlers?.[0]?.fulfilled
    expect(requestHandler).toBeDefined()

    const config = await requestHandler?.({
      headers: new HeadersAdapter(),
    } as never)

    expect(config?.headers.get('Authorization')).toBe('Bearer token-1')
    expect(config?.headers.get('X-Request-Id')).toBeTruthy()
    clearAuthState()
  })

  it('dispatches unauthorized event and clears token on 401', async () => {
    setAuthToken('token-1')
    const listener = vi.fn()
    window.addEventListener('agentweave:unauthorized', listener)
    const responseHandler = httpClient.interceptors.response.handlers?.[0]?.rejected
    expect(responseHandler).toBeDefined()

    await expect(
      responseHandler?.({
        response: {
          status: 401,
          data: {
            code: 'AUTH_401',
            message: 'Unauthorized',
            path: '/api/v1/auth/me',
            traceId: 'trace-1',
            timestamp: new Date().toISOString(),
          },
          headers: {},
        },
        config: { url: '/auth/me' },
      }),
    ).rejects.toMatchObject({ code: 'AUTH_401', status: 401 })

    expect(listener).toHaveBeenCalled()
    expect(localStorage.getItem('agentweave.access_token')).toBeNull()
    window.removeEventListener('agentweave:unauthorized', listener)
  })
})

class HeadersAdapter {
  private readonly values = new Map<string, string>()

  set(key: string, value: string): void {
    this.values.set(key, value)
  }

  get(key: string): string | undefined {
    return this.values.get(key)
  }
}
