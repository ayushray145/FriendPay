import { apiRequest, apiUrl } from './api'

export type DashboardPersonBalance = {
  personId: string
  displayName: string
  netBalance: number
}

export type DashboardData = {
  totalOwedToYou: number
  totalYouOwe: number
  netBalance: number
  people: DashboardPersonBalance[]
}

export type CurrentUser = {
  id: string
  email: string
  displayName: string
}

export class ApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message)
    this.name = 'ApiError'
  }
}

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(apiUrl(path), { credentials: 'include' })
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string } | null
    throw new ApiError(
      `${path} returned ${response.status}${body?.message ? `: ${body.message}` : ''}`,
      response.status,
    )
  }
  return response.json() as Promise<T>
}

export const getDashboard = () => getJson<DashboardData>('/api/v1/dashboard')
export const getCurrentUser = () => getJson<CurrentUser>('/api/v1/auth/me')
export const logout = () => apiRequest<void>('/api/v1/auth/logout', { method: 'POST' })
