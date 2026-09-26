import { useEffect, useState } from 'react'
import { getPaymentProfile, type PaymentProfile } from '../services/payments'
import type { CurrentUser } from '../services/dashboard'

export default function ProfilePanel({ user }: { user: CurrentUser }) {
  const [paymentProfile, setPaymentProfile] = useState<PaymentProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    getPaymentProfile().then((profile) => {
      if (active) setPaymentProfile(profile)
    }).catch((cause: unknown) => {
      if (active) setError(cause instanceof Error ? cause.message : 'Could not load your payment profile.')
    }).finally(() => {
      if (active) setLoading(false)
    })
    return () => { active = false }
  }, [])

  return (
    <section className="profile-page" aria-labelledby="profile-title">
      <div className="welcome-row page-welcome"><div><p className="eyebrow">Account</p><h1 id="profile-title">Profile</h1>
        <p className="muted">Your account details and saved payment information.</p></div></div>
      <section className="profile-details-card" aria-label="Profile details">
        {loading ? <p className="group-loading" aria-live="polite">Loading profile…</p> : error ?
          <p className="payment-feedback error" role="alert">{error}</p> : <dl className="profile-details">
            <div><dt>Name</dt><dd>{user.displayName || 'Not available'}</dd></div>
            <div><dt>Email</dt><dd>{user.email || 'Not available'}</dd></div>
            <div><dt>Username</dt><dd>Not set</dd></div>
            <div><dt>Phone number</dt><dd>Not provided</dd></div>
            <div><dt>Saved UPI ID</dt><dd>{paymentProfile?.upiId || 'None saved'}</dd></div>
          </dl>}
      </section>
    </section>
  )
}
