import { useState, type FormEvent } from 'react'
import { createExpense, type DebtDirection, type PersonContact } from '../services/ledger'

export default function AddExpenseForm({ people, onCreated }: {
  people: PersonContact[]
  onCreated: () => void
}) {
  const [personId, setPersonId] = useState('')
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [direction, setDirection] = useState<DebtDirection>('PERSON_OWES_USER')
  const [working, setWorking] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const selectedPersonId = people.some((person) => person.id === personId) ? personId : (people[0]?.id ?? '')

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedPersonId) return
    setWorking(true)
    setError('')
    setNotice('')
    try {
      await createExpense(selectedPersonId, amount, description, direction)
      const name = people.find((person) => person.id === selectedPersonId)?.displayName ?? 'Person'
      setAmount('')
      setDescription('')
      setNotice(`Expense added to ${name}'s ledger.`)
      onCreated()
    } catch (cause) {
      setError(message(cause))
    } finally {
      setWorking(false)
    }
  }

  return (
    <section className="ledger-action-card primary-action-card" id="add-expense" aria-labelledby="add-expense-title">
      <h3 id="add-expense-title">Add an expense</h3>
      <p className="muted">Quickly record who owes whom and why.</p>
      {people.length === 0 ? <p className="ledger-action-empty">Add a person first to record an expense.</p> : (
        <form className="ledger-action-form" onSubmit={(event) => void onSubmit(event)}>
          <label htmlFor="expense-person">Person</label>
          <select id="expense-person" value={selectedPersonId} onChange={(event) => setPersonId(event.target.value)}>
            {people.map((person) => <option key={person.id} value={person.id}>{person.displayName}</option>)}
          </select>
          <label htmlFor="expense-amount">Amount in rupees</label>
          <input id="expense-amount" type="number" min="0.01" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} placeholder="0.00" required />
          <label htmlFor="expense-description">What was it for?</label>
          <input id="expense-description" value={description} onChange={(event) => setDescription(event.target.value)} maxLength={500} placeholder="Dinner, tickets, cab…" required />
          <label htmlFor="expense-direction">Who owes the money?</label>
          <select id="expense-direction" value={direction} onChange={(event) => setDirection(event.target.value as DebtDirection)}>
            <option value="PERSON_OWES_USER">They owe me</option>
            <option value="USER_OWES_PERSON">I owe them</option>
          </select>
          <button className="small-button" disabled={working}>{working ? 'Saving…' : 'Save expense'}</button>
        </form>
      )}
      {error && <p className="payment-feedback error" role="alert">{error}</p>}
      {notice && <p className="payment-feedback success" role="status">{notice}</p>}
    </section>
  )
}

function message(cause: unknown) {
  return cause instanceof Error ? cause.message : 'Could not save this expense. Please try again.'
}
