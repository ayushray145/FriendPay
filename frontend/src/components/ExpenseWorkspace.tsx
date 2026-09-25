import { useEffect, useState } from 'react'
import AddExpenseForm from './AddExpenseForm'
import AddPersonForm from './AddPersonForm'
import { getPeople, type PersonContact } from '../services/ledger'

export default function ExpenseWorkspace({ onExpenseCreated }: { onExpenseCreated: () => void }) {
  const [people, setPeople] = useState<PersonContact[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    getPeople().then((savedPeople) => {
      if (active) setPeople(savedPeople)
    }).catch((cause: unknown) => {
      if (active) setError(cause instanceof Error ? cause.message : 'Could not load your people.')
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  return (
    <>
      <section className="welcome-row page-welcome"><div><p className="eyebrow">Personal ledger</p><h1>Add expense</h1>
        <p className="muted">Record a private expense with a friend or contact.</p></div></section>
      {loading ? <p className="muted" aria-live="polite">Loading your contacts…</p> : error ?
        <section className="message-card" role="alert"><p>{error}</p><button className="quiet-button" onClick={() => window.location.reload()}>Try again</button></section> :
        <section className="ledger-actions-grid" aria-label="Expense and contact actions">
          <AddExpenseForm people={people} onCreated={onExpenseCreated} />
          <AddPersonForm onCreated={(person) => setPeople((current) => [...current, person]
            .sort((left, right) => left.displayName.localeCompare(right.displayName)))} />
        </section>}
    </>
  )
}
