const DEFAULT_AUTH_REDIRECT = '/app/chat'

export function resolveAuthRedirect(value: unknown): string {
  if (typeof value !== 'string') {
    return DEFAULT_AUTH_REDIRECT
  }

  const redirect = value.trim()
  if (!redirect || redirect === '/login' || redirect.startsWith('/login?')) {
    return DEFAULT_AUTH_REDIRECT
  }

  if (!redirect.startsWith('/') || redirect.startsWith('//')) {
    return DEFAULT_AUTH_REDIRECT
  }

  return redirect
}
