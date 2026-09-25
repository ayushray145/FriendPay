import { apiRequest } from './api'

export type Friend = {
  requestId: string
  userId: string
  email: string
  displayName: string
  nickname: string
  friendsSince: string
}

export type FriendRequest = {
  id: string
  direction: 'INCOMING' | 'OUTGOING'
  status: 'PENDING'
  email: string
  displayName: string
  nickname: string | null
  createdAt: string
}

export const getFriends = () => apiRequest<Friend[]>('/api/v1/friends')
export const getFriendRequests = () => apiRequest<FriendRequest[]>('/api/v1/friends/requests')

export const sendFriendRequest = (email: string, nickname: string) => apiRequest<FriendRequest>('/api/v1/friends/requests', {
  method: 'POST', body: JSON.stringify({ email, ...(nickname.trim() ? { nickname: nickname.trim() } : {}) }),
})

export const acceptFriendRequest = (id: string) => apiRequest<Friend>(`/api/v1/friends/requests/${id}/accept`, { method: 'POST' })
export const declineFriendRequest = (id: string) => apiRequest<void>(`/api/v1/friends/requests/${id}/decline`, { method: 'POST' })
export const updateFriendNickname = (id: string, nickname: string) => apiRequest<Friend>(`/api/v1/friends/${id}/nickname`, {
  method: 'PATCH', body: JSON.stringify({ nickname }),
})
