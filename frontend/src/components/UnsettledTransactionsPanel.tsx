import { useEffect, useState } from 'react'
import { getDashboard } from '../services/dashboard'
import { getPeople } from '../services/ledger'
import { getFriendUpiPaymentLink } from '../services/payments'
import {
  approveFriendSettlementReport, createFriendSettlementReport, getFriendSettlementReports, getFriends,
  rejectFriendSettlementReport, type Friend, type FriendSettlementReport,
} from '../services/friends'

const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 })
type FriendBalance = { personId: string | null; netBalance: number }
type PaymentToConfirm = { friendUserId: string; personId: string; amount: string; upiUri: string }

export default function UnsettledTransactionsPanel({ onLedgerChanged }: { onLedgerChanged: () => void }) {
  const [friends, setFriends] = useState<Friend[]>([])
  const [reports, setReports] = useState<FriendSettlementReport[]>([])
  const [balances, setBalances] = useState<Record<string, FriendBalance>>({})
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [payment, setPayment] = useState<PaymentToConfirm | null>(null)
  const [paymentReference, setPaymentReference] = useState('')

  async function refresh() {
    setLoading(true)
    setError('')
    try {
      const [friendList, people, dashboard, paymentReports] = await Promise.all([
        getFriends(), getPeople(), getDashboard(), getFriendSettlementReports(),
      ])
      const personIdByFriendId = new Map(people.filter((person) => person.linkedUserId).map((person) => [person.linkedUserId!, person.id]))
      const balanceByPersonId = new Map(dashboard.people.map((person) => [person.personId, person.netBalance]))
      setFriends(friendList)
      setReports(paymentReports)
      setBalances(Object.fromEntries(friendList.map((friend) => {
        const personId = personIdByFriendId.get(friend.userId) ?? null
        return [friend.userId, { personId, netBalance: personId ? balanceByPersonId.get(personId) ?? 0 : 0 }]
      })))
    } catch (cause) {
      setError(message(cause, 'Could not load unsettled balances. Please try again.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void refresh() }, [])

  async function beginPayment(friend: Friend) {
    const balance = balances[friend.userId]
    if (!balance?.personId || balance.netBalance >= 0) return
    setWorking(true); setError(''); setNotice(''); setPayment(null); setPaymentReference('')
    const amount = Math.abs(balance.netBalance).toFixed(2)
    try {
      const link = await getFriendUpiPaymentLink(friend.userId, { amount, note: `Split Ledger: ${friend.nickname}` })
      setPayment({ friendUserId: friend.userId, personId: balance.personId, amount, upiUri: link.upiUri })
    } catch (cause) {
      setError(message(cause, 'Could not prepare this UPI payment.'))
    } finally { setWorking(false) }
  }

  async function confirmPayment() {
    if (!payment) return
    setWorking(true); setError(''); setNotice('')
    try {
      await createFriendSettlementReport(payment.friendUserId, payment.amount, paymentReference.trim())
      const friend = friends.find((item) => item.userId === payment.friendUserId)
      setPayment(null)
      setPaymentReference('')
      setNotice(`Payment report sent${friend ? ` to ${friend.nickname}` : ''}. The balance will clear after they approve it.`)
      await refresh()
    } catch (cause) {
      setError(message(cause, 'Could not send the payment report. Refresh the balance and try again.'))
    } finally { setWorking(false) }
  }

  async function reviewReport(report: FriendSettlementReport, action: 'approve' | 'reject') {
    setWorking(true); setError(''); setNotice('')
    try {
      if (action === 'approve') {
        await approveFriendSettlementReport(report.id)
        setNotice(`Approved ${money.format(report.amount)} from ${report.otherNickname}. The settlement is now recorded.`)
        onLedgerChanged()
      } else {
        await rejectFriendSettlementReport(report.id)
        setNotice(`Rejected the payment report from ${report.otherNickname}. The balance remains unsettled.`)
      }
      await refresh()
    } catch (cause) {
      setError(message(cause, 'Could not review this payment report. Please refresh and try again.'))
    } finally { setWorking(false) }
  }

  const owingYou = friends.filter((friend) => (balances[friend.userId]?.netBalance ?? 0) > 0)
  const youOwe = friends.filter((friend) => (balances[friend.userId]?.netBalance ?? 0) < 0)
  const unsettledCount = owingYou.length + youOwe.length
  const incomingReports = reports.filter((report) => report.direction === 'INCOMING' && report.status === 'PENDING')
  const outgoingReports = reports.filter((report) => report.direction === 'OUTGOING')

  return <div className="friends-page unsettled-page">
    <section className="welcome-row people-page-heading">
      <div><p className="eyebrow">Balances with friends</p><h1>Unsettled</h1><p className="muted">See who owes you and what you owe. Expenses awaiting approval are under Expenses.</p></div>
    </section>
    {incomingReports.length > 0 && <section className="friend-list-card settlement-approval-card" aria-labelledby="settlement-approvals-title">
      <div className="panel-heading"><div><p className="eyebrow">Review payments</p><h2 id="settlement-approvals-title">Needs your approval</h2><p className="muted">Check your bank or UPI app before approving. Only your approval clears the balance.</p></div><span className="count-pill">{incomingReports.length}</span></div>
      <ul className="settlement-report-list">{incomingReports.map((report) => <li key={report.id}>
        <div className="settlement-report-identity"><strong>{report.otherNickname}</strong><small>{report.otherName} says they paid you</small>{report.paymentReference && <small>UPI reference: {report.paymentReference}</small>}</div>
        <strong className="settlement-report-amount">{money.format(report.amount)}</strong>
        <div className="settlement-report-actions"><button className="small-button" disabled={working} onClick={() => void reviewReport(report, 'approve')}>Approve payment</button><button className="quiet-button" disabled={working} onClick={() => void reviewReport(report, 'reject')}>Reject</button></div>
      </li>)}</ul>
    </section>}
    <section className="friend-list-card pending-friend-balances unsettled-card" aria-labelledby="unsettled-title">
      <div className="panel-heading"><div><p className="eyebrow">Unsettled balances</p><h2 id="unsettled-title">Pending transactions</h2><p className="muted">Balances include approved expenses and settlements confirmed by the lender.</p></div><span className="count-pill">{unsettledCount}</span></div>
      {loading ? <p className="friend-state" aria-live="polite">Loading balances…</p> : friends.length === 0 ?
        <p className="friend-state">Accept a friend request to see shared balances here.</p> : <div className="friend-balance-columns">
          <FriendBalanceGroup title="They owe you" friends={owingYou} balances={balances} empty="No friends currently owe you." />
          <FriendBalanceGroup title="You owe them" friends={youOwe} balances={balances} empty="You don’t currently owe any friends." working={working} payment={payment} reports={reports} paymentReference={paymentReference} onPaymentReferenceChange={setPaymentReference} onPay={(friend) => void beginPayment(friend)} onConfirm={() => void confirmPayment()} />
        </div>}
      {outgoingReports.length > 0 && <section className="outgoing-reports" aria-label="Payment reports you sent"><h3>Payment reports you sent</h3><ul className="settlement-report-history">{outgoingReports.map((report) => <li key={report.id}>
        <span><strong>{report.otherNickname}</strong><small>{money.format(report.amount)} · {new Date(report.createdAt).toLocaleDateString()}</small></span>
        <span className={`settlement-report-status ${report.status.toLowerCase()}`}>{report.status === 'PENDING' ? 'Waiting for approval' : report.status === 'APPROVED' ? 'Approved' : 'Rejected'}</span>
      </li>)}</ul></section>}
      {error && <p className="payment-feedback error" role="alert">{error}</p>}
      {notice && <p className="payment-feedback success" role="status">{notice}</p>}
      {!loading && <button className="quiet-button friend-refresh" onClick={() => void refresh()}>Refresh balances</button>}
    </section>
  </div>
}

function FriendBalanceGroup({ title, friends, balances, empty, working = false, payment = null, reports = [], paymentReference = '', onPaymentReferenceChange, onPay, onConfirm }: {
  title: string; friends: Friend[]; balances: Record<string, FriendBalance>; empty: string; working?: boolean
  payment?: PaymentToConfirm | null; reports?: FriendSettlementReport[]
  paymentReference?: string; onPaymentReferenceChange?: (reference: string) => void
  onPay?: (friend: Friend) => void; onConfirm?: () => void
}) {
  return <section className="friend-balance-group" aria-label={title}><h3>{title}</h3>
    {friends.length === 0 ? <p className="friend-balance-empty">{empty}</p> : <ul className="friend-balance-list">{friends.map((friend) => {
      const friendBalance = balances[friend.userId]
      const currentPayment = payment?.friendUserId === friend.userId ? payment : null
      const pendingReport = reports.find((report) => report.direction === 'OUTGOING'
        && report.otherUserId === friend.userId && report.status === 'PENDING')
      return <li key={friend.requestId}><div className="friend-balance-main"><span className="friend-balance-person"><strong>{friend.nickname}</strong><small>{friend.email}</small></span><strong className="friend-balance-value">{money.format(Math.abs(friendBalance?.netBalance ?? 0))}</strong></div>
        {onPay && <div className="friend-balance-payment-actions">{currentPayment ? <>
          <a className="small-button upi-open-button" href={currentPayment.upiUri}>Pay now — open UPI app</a>
          <label className="payment-reference-field">UPI transaction reference (optional)<input value={paymentReference} maxLength={80} pattern="[A-Za-z0-9._/-]*" onChange={(event) => onPaymentReferenceChange?.(event.target.value)} placeholder="Enter the reference from your UPI app" /></label>
          <button className="small-button" disabled={working} onClick={onConfirm}>I paid — request approval</button><small className="payment-confirmation-hint">Your friend checks their account before approving. The balance remains until then.</small>
        </> : pendingReport ? <small className="upi-sharing-needed">Payment reported — waiting for {friend.nickname} to approve</small> : <>
          <button className="small-button" disabled={working || !friendBalance?.personId || !friend.canReceivePayments} onClick={() => onPay(friend)}>{working ? 'Preparing…' : 'Pay now'}</button>
          {!friend.canReceivePayments && <small className="upi-sharing-needed">Your friend needs to share their UPI details in UPI payments before you can pay here.</small>}
        </>}</div>}
      </li>
    })}</ul>}
  </section>
}

function message(cause: unknown, fallback: string) { return cause instanceof Error ? cause.message : fallback }
