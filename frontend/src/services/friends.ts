import { apiRequest } from './api'

export type Friend = {
  requestId: string
  userId: string
  email: string
  displayName: string
  nickname: string
  friendsSince: string
  canReceivePayments: boolean
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

export type FriendExpenseProposal = {
  id: string
  requestDirection: 'INCOMING' | 'OUTGOING'
  status: 'PENDING' | 'DISPUTED'
  otherUserId: string
  otherName: string
  otherNickname: string
  amount: number
  description: string
  debtDirection: 'PERSON_OWES_USER' | 'USER_OWES_PERSON'
  disputeReason: string | null
  createdAt: string
}

export const getFriends = () => apiRequest<Friend[]>('/api/v1/friends')
export const getFriendExpenseProposals = () => apiRequest<FriendExpenseProposal[]>('/api/v1/friend-expenses')
export const createFriendExpenseProposal = (friendUserId: string, amount: string, description: string,
                                             debtDirection: 'PERSON_OWES_USER' | 'USER_OWES_PERSON') =>
  apiRequest<FriendExpenseProposal>('/api/v1/friend-expenses', {
    method: 'POST', body: JSON.stringify({ friendUserId, amount, description, debtDirection }),
  })
export const approveFriendExpenseProposal = (id: string) =>
  apiRequest<FriendExpenseProposal>(`/api/v1/friend-expenses/${id}/approve`, { method: 'POST' })
export const disputeFriendExpenseProposal = (id: string, reason: string) =>
  apiRequest<FriendExpenseProposal>(`/api/v1/friend-expenses/${id}/dispute`, {
    method: 'POST', body: JSON.stringify({ reason }),
  })
export const reviseFriendExpenseProposal = (id: string, amount: string, description: string,
                                              debtDirection: 'PERSON_OWES_USER' | 'USER_OWES_PERSON') =>
  apiRequest<FriendExpenseProposal>(`/api/v1/friend-expenses/${id}`, {
    method: 'PATCH', body: JSON.stringify({ amount, description, debtDirection }),
  })
export const getFriendRequests = () => apiRequest<FriendRequest[]>('/api/v1/friends/requests')

export const sendFriendRequest = (email: string, nickname: string) => apiRequest<FriendRequest>('/api/v1/friends/requests', {
  method: 'POST', body: JSON.stringify({ email, ...(nickname.trim() ? { nickname: nickname.trim() } : {}) }),
})

export const acceptFriendRequest = (id: string) => apiRequest<Friend>(`/api/v1/friends/requests/${id}/accept`, { method: 'POST' })
export const declineFriendRequest = (id: string) => apiRequest<void>(`/api/v1/friends/requests/${id}/decline`, { method: 'POST' })
export const updateFriendNickname = (id: string, nickname: string) => apiRequest<Friend>(`/api/v1/friends/${id}/nickname`, {
  method: 'PATCH', body: JSON.stringify({ nickname }),
})
