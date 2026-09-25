import { apiRequest } from './api'

export type GroupMember = {
  membershipId: string
  userId: string
  displayName: string
  role: 'OWNER' | 'MEMBER'
  status: 'ACTIVE' | 'REMOVED'
  joinedAt: string | null
}

export type LedgerGroup = {
  id: string
  name: string
  createdAt: string
  members: GroupMember[]
}

export type GroupExpense = {
  id: string
  groupId: string
  amount: number
  description: string
  paidByUserId: string
  paidByName: string
  recordedByUserId: string
  occurredAt: string
  shares: { userId: string; displayName: string; shareAmount: number }[]
}

export type GroupBalance = { userId: string; displayName: string; netBalance: number }

export type GroupDispute = {
  id: string
  groupId: string
  issueType: 'WRONGLY_ADDED' | 'INCORRECT_AMOUNT'
  status: 'OPEN' | 'RESOLVED'
  raisedByUserId: string
  raisedByName: string
  groupExpenseId: string | null
  groupExpenseDescription: string | null
  createdAt: string
  resolvedAt: string | null
  resolvedByName: string | null
}

export const getGroups = () => apiRequest<LedgerGroup[]>('/api/v1/groups')
export const createGroup = (name: string) => apiRequest<LedgerGroup>('/api/v1/groups', {
  method: 'POST', body: JSON.stringify({ name }),
})
export const addGroupMember = (groupId: string, email: string) => apiRequest<GroupMember>(`/api/v1/groups/${groupId}/members`, {
  method: 'POST', body: JSON.stringify({ email }),
})
export const removeGroupMember = (groupId: string, memberId: string) => apiRequest<void>(
  `/api/v1/groups/${groupId}/members/${memberId}`, { method: 'DELETE' },
)
export const getGroupExpenses = (groupId: string) => apiRequest<GroupExpense[]>(`/api/v1/groups/${groupId}/expenses`)
export const getGroupBalances = (groupId: string) => apiRequest<GroupBalance[]>(`/api/v1/groups/${groupId}/balances`)
export const createGroupExpense = (groupId: string, amount: string, description: string,
                                  participantUserIds: string[], paidByUserId: string) =>
  apiRequest<GroupExpense>(`/api/v1/groups/${groupId}/expenses`, {
    method: 'POST', body: JSON.stringify({ amount, description, participantUserIds, paidByUserId }),
  })
export const getGroupDisputes = (groupId: string) => apiRequest<GroupDispute[]>(`/api/v1/groups/${groupId}/disputes`)
export const raiseGroupDispute = (groupId: string, issueType: GroupDispute['issueType'], groupExpenseId?: string) =>
  apiRequest<GroupDispute>(`/api/v1/groups/${groupId}/disputes`, {
    method: 'POST', body: JSON.stringify({ issueType, ...(groupExpenseId ? { groupExpenseId } : {}) }),
  })
export const resolveGroupDispute = (groupId: string, disputeId: string) =>
  apiRequest<GroupDispute>(`/api/v1/groups/${groupId}/disputes/${disputeId}/resolve`, { method: 'POST' })
