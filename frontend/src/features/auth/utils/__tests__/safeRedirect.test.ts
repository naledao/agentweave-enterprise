import { describe, expect, it } from 'vitest'

import { resolveAuthRedirect } from '@/features/auth/utils/safeRedirect'

describe('resolveAuthRedirect', () => {
  it('keeps internal workspace paths', () => {
    expect(resolveAuthRedirect('/app/chat')).toBe('/app/chat')
    expect(resolveAuthRedirect('/app/settings/users?tab=active')).toBe('/app/settings/users?tab=active')
  })

  it('falls back for unsafe or looping redirects', () => {
    expect(resolveAuthRedirect(undefined)).toBe('/app/chat')
    expect(resolveAuthRedirect('https://example.com')).toBe('/app/chat')
    expect(resolveAuthRedirect('//example.com')).toBe('/app/chat')
    expect(resolveAuthRedirect('/login')).toBe('/app/chat')
    expect(resolveAuthRedirect('/login?redirect=/app/chat')).toBe('/app/chat')
  })
})
