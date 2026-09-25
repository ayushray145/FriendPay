export class ApiRequestError extends Error {
  constructor(message: string, readonly status: number) {
    super(message)
    this.name = 'ApiRequestError'
  }
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body) headers.set('Content-Type', 'application/json')
  if (init.method && init.method !== 'GET') {
    const csrfResponse = await fetch('/api/v1/auth/csrf', { credentials: 'include' })
    if (!csrfResponse.ok) throw new Error('Could not prepare a secure request. Refresh and try again.')
    const csrf = await csrfResponse.json() as { token: string; headerName: string }
    headers.set(csrf.headerName, csrf.token)
  }

  const response = await fetch(path, { ...init, headers, credentials: 'include' })
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string } | null
    throw new ApiRequestError(body?.message || `Request failed (${response.status})`, response.status)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}
