import { apiRequest } from './api'

export type PaymentProfile = {
  userId: string
  displayName: string
  upiId: string | null
  updatedAt: string | null
}

export type UpiPaymentRequest = { amount: string; note: string }

export type PersonLedgerEntry = {
  id: string
  type: 'EXPENSE' | 'SETTLEMENT'
  amount: number
  direction: 'PERSON_OWES_USER' | 'USER_OWES_PERSON'
  description: string
  occurredAt: string
}

export const getPaymentProfile = () => apiRequest<PaymentProfile>('/api/v1/payment-profile')
export const savePaymentProfile = (upiId: string) => apiRequest<PaymentProfile>('/api/v1/payment-profile', {
  method: 'PUT', body: JSON.stringify({ upiId }),
})
export const deletePaymentProfile = () => apiRequest<void>('/api/v1/payment-profile', { method: 'DELETE' })

function paymentQuery(request: UpiPaymentRequest) {
  const params = new URLSearchParams({ amount: request.amount, note: request.note })
  return params.toString()
}

export const getUpiPaymentLink = (payment: UpiPaymentRequest) =>
  apiRequest<{ upiUri: string }>(`/api/v1/payment-profile/upi-link?${paymentQuery(payment)}`)

export async function getUpiQr(payment: UpiPaymentRequest): Promise<Blob> {
  const response = await fetch(`/api/v1/payment-profile/qr?${paymentQuery(payment)}`, { credentials: 'include' })
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string } | null
    throw new Error(body?.message || `Request failed (${response.status})`)
  }
  return response.blob()
}

export async function getMyUpiQr(): Promise<Blob> {
  const response = await fetch('/api/v1/payment-profile/my-qr', { credentials: 'include' })
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string } | null
    throw new Error(body?.message || `Request failed (${response.status})`)
  }
  return response.blob()
}

export const getPersonLedger = (personId: string) =>
  apiRequest<PersonLedgerEntry[]>(`/api/v1/people/${personId}/ledger`)

export const createPersonSettlement = (personId: string, amount: string,
                                       paymentDirection: PersonLedgerEntry['direction']) =>
  apiRequest(`/api/v1/people/${personId}/settlements`, {
    method: 'POST', body: JSON.stringify({ amount, paymentDirection }),
  })
