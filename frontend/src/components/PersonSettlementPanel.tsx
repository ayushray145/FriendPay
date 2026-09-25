import { useEffect, useState, type FormEvent } from 'react'
import { createPersonSettlement, getPersonLedger, type PersonLedgerEntry } from '../services/payments'
import type { DashboardPersonBalance } from '../services/dashboard'
import type { UpiPaymentRequest } from '../services/payments'

const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 })

export default function PersonSettlementPanel({ person, onClose, onRecorded, onRequestPayment }: {
  person: DashboardPersonBalance
  onClose: () => void
  onRecorded: () => void
  onRequestPayment: (request: UpiPaymentRequest) => void
}) {
  const [entries, setEntries] = useState<PersonLedgerEntry[]>([])
  const [amount, setAmount] = useState(Math.abs(person.netBalance).toFixed(2))
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const owesYou = person.netBalance > 0
  const outstanding = Math.abs(person.netBalance)

  useEffect(() => {
    let active = true
    getPersonLedger(person.personId)
      .then((ledger) => { if (active) setEntries(ledger) })
      .catch((cause: unknown) => { if (active) setError(message(cause)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [person.personId])

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setWorking(true)
    setError('')
    setNotice('')
    try {
      await createPersonSettlement(person.personId, amount, owesYou ? 'PERSON_OWES_USER' : 'USER_OWES_PERSON')
      setNotice('Confirmed payment recorded. The balance is being refreshed.')
      setAmount('')
      setEntries(await getPersonLedger(person.personId))
      onRecorded()
    } catch (cause) {
      setError(message(cause))
    } finally {
      setWorking(false)
    }
  }

  return (
    <section className="person-ledger-detail" aria-labelledby="ledger-detail-title">
      <div className="person-ledger-heading"><div><p className="eyebrow">Private ledger</p><h3 id="ledger-detail-title">{person.displayName}</h3><p className="muted">{owesYou ? 'They owe you' : 'You owe them'} {money.format(outstanding)}</p></div><button className="quiet-button" onClick={onClose}>Close</button></div>
      {outstanding > 0 && owesYou && <button className="small-button" onClick={() => onRequestPayment({ amount: outstanding.toFixed(2), note: `Split Ledger: ${person.displayName}` })}>Create a UPI request for {money.format(outstanding)}</button>}
      {outstanding > 0 ? <form className="settlement-form" onSubmit={(event) => void onSubmit(event)}>
        <label htmlFor="settlement-amount">{owesYou ? 'Amount they have paid you' : 'Amount you have paid them'}</label>
        <div className="inline-form"><input id="settlement-amount" type="number" min="0.01" max={outstanding.toFixed(2)} step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} required /><button className="small-button" disabled={working}>Record confirmed payment</button></div>
        <p className="form-hint">Only record a payment after you have confirmed it outside this app. A UPI request does not confirm payment.</p>
      </form> : <p className="ledger-no-balance">No outstanding balance with {person.displayName}.</p>}
      {error && <p className="payment-feedback error" role="alert">{error}</p>}
      {notice && <p className="payment-feedback success" role="status">{notice}</p>}
      <div className="ledger-history"><h4>Transaction history</h4>{loading ? <p className="group-loading">Loading history…</p> : entries.length === 0 ? <p className="group-empty">No transactions yet.</p> : <ul>{entries.map((entry) => <li key={entry.id}><span><strong>{entry.description}</strong><small>{entry.type === 'SETTLEMENT' ? 'Confirmed settlement' : entry.direction === 'PERSON_OWES_USER' ? 'They owe you' : 'You owe them'} · {new Date(entry.occurredAt).toLocaleDateString()}</small></span><strong>{money.format(entry.amount)}</strong></li>)}</ul>}</div>
    </section>
  )
}

function message(cause: unknown) {
  return cause instanceof Error ? cause.message : 'Something went wrong. Please try again.'
}
