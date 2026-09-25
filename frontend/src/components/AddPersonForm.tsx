import { useState, type FormEvent } from 'react'
import { createPerson, type PersonContact } from '../services/ledger'

export default function AddPersonForm({ onCreated }: { onCreated: (person: PersonContact) => void }) {
  const [displayName, setDisplayName] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [working, setWorking] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setWorking(true)
    setError('')
    setNotice('')
    try {
      const person = await createPerson(displayName, phoneNumber)
      onCreated(person)
      setDisplayName('')
      setPhoneNumber('')
      setNotice(`${person.displayName} added to your private people list.`)
    } catch (cause) {
      setError(message(cause))
    } finally {
      setWorking(false)
    }
  }

  return (
    <section className="ledger-action-card" aria-labelledby="add-person-title">
      <h3 id="add-person-title">Add a person</h3>
      <p className="muted">A private contact for your ledger. A phone number alone is enough.</p>
      <form className="ledger-action-form" onSubmit={(event) => void onSubmit(event)}>
        <label htmlFor="person-name">Name <span>(optional if you add a phone)</span></label>
        <input id="person-name" value={displayName} onChange={(event) => setDisplayName(event.target.value)} maxLength={160} placeholder="e.g. Rahul" />
        <label htmlFor="person-phone">Phone number <span>(optional)</span></label>
        <input id="person-phone" type="tel" value={phoneNumber} onChange={(event) => setPhoneNumber(event.target.value)} placeholder="9876543210 or +country code" />
        <button className="small-button" disabled={working || (!displayName.trim() && !phoneNumber.trim())}>{working ? 'Adding…' : 'Add person'}</button>
      </form>
      {error && <p className="payment-feedback error" role="alert">{error}</p>}
      {notice && <p className="payment-feedback success" role="status">{notice}</p>}
      <p className="form-hint">Phone numbers identify private contacts only; sign-in remains Google-only.</p>
    </section>
  )
}

function message(cause: unknown) {
  return cause instanceof Error ? cause.message : 'Could not add this person. Please try again.'
}
