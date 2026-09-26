import { apiRequest } from './api'

export type PersonContact = {
  id: string
  displayName: string
  phoneNumber: string | null
  linkedUserId: string | null
  createdAt: string
}

export type DebtDirection = 'PERSON_OWES_USER' | 'USER_OWES_PERSON'

export const getPeople = () => apiRequest<PersonContact[]>('/api/v1/people')

export const createPerson = (displayName: string, phoneNumber: string) =>
  apiRequest<PersonContact>('/api/v1/people', {
    method: 'POST', body: JSON.stringify({
      ...(displayName.trim() ? { displayName: displayName.trim() } : {}),
      ...(phoneNumber.trim() ? { phoneNumber: phoneNumber.trim() } : {}),
    }),
  })

export const createExpense = (personId: string, amount: string, description: string, debtDirection: DebtDirection) =>
  apiRequest(`/api/v1/people/${personId}/expenses`, {
    method: 'POST', body: JSON.stringify({ amount, description, debtDirection }),
  })
