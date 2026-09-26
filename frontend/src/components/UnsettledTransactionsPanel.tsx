import { useEffect, useState } from 'react'
import { getDashboard } from '../services/dashboard'
import { getPeople } from '../services/ledger'
import { createPersonSettlement, getFriendUpiPaymentLink } from '../services/payments'
import { getFriends, type Friend } from '../services/friends'

const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 })
type FriendBalance = { personId: string | null; netBalance: number }
type PaymentToConfirm = { friendUserId: string; personId: string; amount: string; upiUri: string }

export default function UnsettledTransactionsPanel() {
  const [friends, setFriends] = useState<Friend[]>([])
  const [balances, setBalances] = useState<Record<string, FriendBalance>>({})
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [payment, setPayment] = useState<PaymentToConfirm | null>(null)

  async function refresh() {
    setLoading(true)
    setError('')
    try {
      const [friendList, people, dashboard] = await Promise.all([getFriends(), getPeople(), getDashboard()])
      const personIdByFriendId = new Map(people.filter((person) => person.linkedUserId).map((person) => [person.linkedUserId!, person.id]))
      const balanceByPersonId = new Map(dashboard.people.map((person) => [person.personId, person.netBalance]))
      setFriends(friendList)
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
    setWorking(true); setError(''); setNotice(''); setPayment(null)
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
      await createPersonSettlement(payment.personId, payment.amount, 'USER_OWES_PERSON')
      const friend = friends.find((item) => item.userId === payment.friendUserId)
      setPayment(null)
      setNotice(`Payment recorded${friend ? ` with ${friend.nickname}` : ''}.`)
      await refresh()
    } catch (cause) {
      setError(message(cause, 'Could not record the payment. Refresh the balance and try again.'))
    } finally { setWorking(false) }
  }

  const owingYou = friends.filter((friend) => (balances[friend.userId]?.netBalance ?? 0) > 0)
  const youOwe = friends.filter((friend) => (balances[friend.userId]?.netBalance ?? 0) < 0)
  const unsettledCount = owingYou.length + youOwe.length

  return <div className="friends-page unsettled-page">
    <section className="welcome-row people-page-heading">
      <div><p className="eyebrow">Balances with friends</p><h1>Unsettled</h1><p className="muted">See who owes you and what you owe. Expenses awaiting approval are under Expenses.</p></div>
    </section>
    <section className="friend-list-card pending-friend-balances unsettled-card" aria-labelledby="unsettled-title">
      <div className="panel-heading"><div><p className="eyebrow">Unsettled balances</p><h2 id="unsettled-title">Pending transactions</h2><p className="muted">Balances include approved friend expenses less recorded settlements.</p></div><span className="count-pill">{unsettledCount}</span></div>
      {loading ? <p className="friend-state" aria-live="polite">Loading balances…</p> : friends.length === 0 ?
        <p className="friend-state">Accept a friend request to see shared balances here.</p> : <div className="friend-balance-columns">
          <FriendBalanceGroup title="They owe you" friends={owingYou} balances={balances} empty="No friends currently owe you." />
          <FriendBalanceGroup title="You owe them" friends={youOwe} balances={balances} empty="You don’t currently owe any friends." working={working} payment={payment} onPay={(friend) => void beginPayment(friend)} onConfirm={() => void confirmPayment()} />
        </div>}
      {error && <p className="payment-feedback error" role="alert">{error}</p>}
      {notice && <p className="payment-feedback success" role="status">{notice}</p>}
      {!loading && <button className="quiet-button friend-refresh" onClick={() => void refresh()}>Refresh balances</button>}
    </section>
  </div>
}

function FriendBalanceGroup({ title, friends, balances, empty, working = false, payment = null, onPay, onConfirm }: {
  title: string; friends: Friend[]; balances: Record<string, FriendBalance>; empty: string; working?: boolean
  payment?: PaymentToConfirm | null; onPay?: (friend: Friend) => void; onConfirm?: () => void
}) {
  return <section className="friend-balance-group" aria-label={title}><h3>{title}</h3>
    {friends.length === 0 ? <p className="friend-balance-empty">{empty}</p> : <ul className="friend-balance-list">{friends.map((friend) => {
      const friendBalance = balances[friend.userId]
      const currentPayment = payment?.friendUserId === friend.userId ? payment : null
      return <li key={friend.requestId}><div className="friend-balance-main"><span className="friend-balance-person"><strong>{friend.nickname}</strong><small>{friend.email}</small></span><strong className="friend-balance-value">{money.format(Math.abs(friendBalance?.netBalance ?? 0))}</strong></div>
        {onPay && <div className="friend-balance-payment-actions">{currentPayment ? <>
          <a className="small-button upi-open-button" href={currentPayment.upiUri}>Pay now — open UPI app</a><button className="small-button" disabled={working} onClick={onConfirm}>I paid — clear due</button><small className="payment-confirmation-hint">Confirm only after completing payment in your UPI app.</small>
        </> : <>
          <button className="small-button" disabled={working || !friendBalance?.personId || !friend.canReceivePayments} onClick={() => onPay(friend)}>{working ? 'Preparing…' : 'Pay now'}</button>
          {!friend.canReceivePayments && <small className="upi-sharing-needed">Your friend needs to share their UPI details in UPI payments before you can pay here.</small>}
        </>}</div>}
      </li>
    })}</ul>}
  </section>
}

function message(cause: unknown, fallback: string) { return cause instanceof Error ? cause.message : fallback }
