import { useEffect, useState, type FormEvent } from 'react'
import {
  deletePaymentProfile,
  getPaymentProfile,
  getMyUpiQr,
  getUpiPaymentLink,
  getUpiQr,
  savePaymentProfile,
  type PaymentProfile,
  type UpiPaymentRequest,
} from '../services/payments'

export default function PaymentsPanel({ suggestedPayment }: { suggestedPayment: UpiPaymentRequest | null }) {
  const [profile, setProfile] = useState<PaymentProfile | null>(null)
  const [upiId, setUpiId] = useState('')
  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('Split Ledger settlement')
  const [qrUrl, setQrUrl] = useState('')
  const [paymentUri, setPaymentUri] = useState('')
  const [myQrUrl, setMyQrUrl] = useState('')
  const [myQrLoading, setMyQrLoading] = useState(false)
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let active = true
    getPaymentProfile()
      .then((savedProfile) => {
        if (!active) return
        setProfile(savedProfile)
        setUpiId(savedProfile.upiId ?? '')
      })
      .catch((cause: unknown) => { if (active) setError(message(cause)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (suggestedPayment) {
      setAmount(suggestedPayment.amount)
      setNote(suggestedPayment.note)
      document.getElementById('payments')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }, [suggestedPayment])

  useEffect(() => () => { if (qrUrl) URL.revokeObjectURL(qrUrl) }, [qrUrl])
  useEffect(() => () => { if (myQrUrl) URL.revokeObjectURL(myQrUrl) }, [myQrUrl])

  async function onToggleMyQr() {
    if (myQrUrl) {
      URL.revokeObjectURL(myQrUrl)
      setMyQrUrl('')
      return
    }
    setMyQrLoading(true)
    setError('')
    try {
      setMyQrUrl(URL.createObjectURL(await getMyUpiQr()))
    } catch (cause) {
      setError(message(cause))
    } finally {
      setMyQrLoading(false)
    }
  }

  async function onSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setWorking(true)
    setError('')
    setNotice('')
    try {
      const saved = await savePaymentProfile(upiId.trim())
      setProfile(saved)
      setUpiId(saved.upiId ?? '')
      setNotice('UPI ID saved to your private payment profile.')
    } catch (cause) {
      setError(message(cause))
    } finally {
      setWorking(false)
    }
  }

  async function onDelete() {
    setWorking(true)
    setError('')
    setNotice('')
    try {
      await deletePaymentProfile()
      setProfile((current) => current ? { ...current, upiId: null, updatedAt: null } : null)
      setUpiId('')
      setPaymentUri('')
      setQrUrl('')
      setNotice('UPI ID removed.')
    } catch (cause) {
      setError(message(cause))
    } finally {
      setWorking(false)
    }
  }

  async function onGenerate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setWorking(true)
    setError('')
    setNotice('')
    setQrUrl('')
    setPaymentUri('')
    try {
      const request = { amount, note }
      const [link, image] = await Promise.all([getUpiPaymentLink(request), getUpiQr(request)])
      setPaymentUri(link.upiUri)
      setQrUrl(URL.createObjectURL(image))
    } catch (cause) {
      setError(message(cause))
    } finally {
      setWorking(false)
    }
  }

  return (
    <section className="payments-panel" id="payments" aria-labelledby="payments-title">
      <div className="payments-heading"><div><p className="eyebrow">Payment setup</p><h2 id="payments-title">UPI payments</h2><p className="muted">Save a UPI ID to let people pay you from their UPI app.</p></div>
        <button className="small-button my-qr-button" type="button" onClick={() => void onToggleMyQr()} disabled={!profile?.upiId || myQrLoading}>
          {myQrLoading ? 'Loading…' : myQrUrl ? 'Hide my QR' : 'My QR'}
        </button>
      </div>
      {myQrUrl && <div className="my-qr-card"><img src={myQrUrl} alt="Your UPI QR code with no preset amount" />
        <div><h3>My UPI QR</h3><p className="muted">Anyone can scan this in their UPI app to pay you. The amount is entered by the payer.</p>
          <strong>{profile?.upiId}</strong></div></div>}
      {error && <p className="payment-feedback error" role="alert">{error}</p>}
      {notice && <p className="payment-feedback success" role="status">{notice}</p>}
      {loading ? <p className="group-loading">Loading payment profile…</p> : (
        <div className="payments-layout">
          <div className="payment-profile-card">
            <h3>Your payment profile</h3>
            <form onSubmit={onSave} className="payment-profile-form">
              <label htmlFor="upi-id">UPI ID</label>
              <div className="upi-id-field">
                <input id="upi-id" value={upiId} onChange={(event) => setUpiId(event.target.value)} placeholder="name@bank" maxLength={320} required disabled={Boolean(profile?.upiId)} />
                {profile?.upiId ? <span className="upi-saved-check" aria-label="UPI ID saved">✓</span> :
                  <button className="small-button" disabled={working}>Add UPI ID</button>}
              </div>
            </form>
            {profile?.upiId && <button className="quiet-button danger-text" disabled={working} onClick={() => void onDelete()}>Remove UPI ID</button>}
            <p className="form-hint">This is stored privately on your account. Split Ledger never processes the payment.</p>
          </div>
          <div className="payment-request-card">
            <h3>Create a payment request</h3>
            <form className="payment-request-form" onSubmit={(event) => void onGenerate(event)}>
              <div className="payment-request-fields"><label>Amount in rupees<input type="number" min="0.01" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} required /></label><label>Note<input value={note} onChange={(event) => setNote(event.target.value)} maxLength={80} /></label></div>
              <button className="small-button" disabled={working || !profile?.upiId}>Create QR and UPI link</button>
            </form>
            {!profile?.upiId && <p className="form-hint">Add your UPI ID first to create a payment request.</p>}
            {qrUrl && <div className="payment-qr-result"><img src={qrUrl} alt="UPI payment QR code" /><div><p>Scan with a UPI app to open a payment request. Check the amount and recipient in the app before paying.</p><a className="small-button upi-open-button" href={paymentUri}>Open UPI app</a></div></div>}
            <p className="payment-safety-note">Creating or opening this request does not record a settlement. Record a settlement only after payment is confirmed outside Split Ledger.</p>
          </div>
        </div>
      )}
    </section>
  )
}

function message(cause: unknown) {
  return cause instanceof Error ? cause.message : 'Something went wrong. Please try again.'
}
