import { useEffect, useState, type FormEvent } from 'react'
import {
  approveFriendExpenseProposal,
  disputeFriendExpenseProposal,
  getFriendExpenseProposals,
  reviseFriendExpenseProposal,
  type FriendExpenseProposal,
} from '../services/friends'

const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 })

export default function FriendExpenseRequestsPanel({ refreshKey, onChanged }: { refreshKey: number; onChanged: () => void }) {
  const [proposals, setProposals] = useState<FriendExpenseProposal[]>([])
  const [loading, setLoading] = useState(true)
  const [workingId, setWorkingId] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function refresh() {
    setError('')
    try {
      setProposals(await getFriendExpenseProposals())
    } catch (cause) {
      setError(message(cause))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void refresh() }, [refreshKey])

  async function perform(id: string, action: () => Promise<unknown>, success: string) {
    setWorkingId(id)
    setError('')
    setNotice('')
    try {
      await action()
      setNotice(success)
      await refresh()
      onChanged()
    } catch (cause) {
      setError(message(cause))
    } finally {
      setWorkingId('')
    }
  }

  return (
    <section className="friend-expense-requests" aria-labelledby="friend-expense-requests-title">
      <div className="friend-expense-requests-heading"><div><p className="eyebrow">Shared with friends</p><h2 id="friend-expense-requests-title">Friend expenses</h2>
        <p className="muted">Expenses need your friend’s approval before they affect either balance.</p></div><span className="count-pill">{proposals.length} pending</span></div>
      {error && <p className="payment-feedback error" role="alert">{error}</p>}
      {notice && <p className="payment-feedback success" role="status">{notice}</p>}
      {loading ? <p className="friend-expense-empty" aria-live="polite">Loading friend expenses…</p> : proposals.length === 0 ?
        <p className="friend-expense-empty">No friend expense requests.</p> : <ul className="friend-expense-list">
          {proposals.map((proposal) => <ProposalItem key={proposal.id} proposal={proposal} working={workingId === proposal.id}
            onApprove={() => perform(proposal.id, () => approveFriendExpenseProposal(proposal.id), 'Expense approved and added to both ledgers.')}
            onDispute={(reason) => perform(proposal.id, () => disputeFriendExpenseProposal(proposal.id, reason), 'Expense disputed. The sender can edit and resubmit it.')}
            onRevise={(amount, description, direction) => perform(proposal.id,
              () => reviseFriendExpenseProposal(proposal.id, amount, description, direction), 'Revised expense sent for approval again.')} />)}
        </ul>}
    </section>
  )
}

function ProposalItem({ proposal, working, onApprove, onDispute, onRevise }: {
  proposal: FriendExpenseProposal
  working: boolean
  onApprove: () => void
  onDispute: (reason: string) => void
  onRevise: (amount: string, description: string, direction: FriendExpenseProposal['debtDirection']) => void
}) {
  const [showDisputeForm, setShowDisputeForm] = useState(false)
  const [reason, setReason] = useState('')
  const [amount, setAmount] = useState(proposal.amount.toFixed(2))
  const [description, setDescription] = useState(proposal.description)
  const [direction, setDirection] = useState(proposal.debtDirection)
  const incoming = proposal.requestDirection === 'INCOMING'

  function submitDispute(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onDispute(reason.trim())
    setShowDisputeForm(false)
    setReason('')
  }

  function submitRevision(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onRevise(amount, description.trim(), direction)
  }

  return (
    <li className="friend-expense-item">
      <div className="friend-expense-copy"><div className="friend-expense-title"><strong>{proposal.otherNickname}</strong>
        <span className={`friend-expense-status ${proposal.status.toLowerCase()}`}>{proposal.status === 'DISPUTED' ? 'Disputed' : 'Awaiting approval'}</span></div>
        <span>{proposal.description} · {proposal.debtDirection === 'PERSON_OWES_USER' ? 'They owe you' : 'You owe them'}</span>
        <small>{money.format(proposal.amount)} · {incoming ? 'Sent to you' : 'Sent by you'}</small>
        {proposal.status === 'DISPUTED' && proposal.disputeReason && <small className="friend-dispute-reason">Dispute: {proposal.disputeReason}</small>}
      </div>
      {incoming && proposal.status === 'PENDING' && <div className="friend-expense-actions">
        <button className="small-button" type="button" disabled={working} onClick={onApprove}>{working ? 'Saving…' : 'Approve'}</button>
        {!showDisputeForm && <button className="quiet-button" type="button" disabled={working} onClick={() => setShowDisputeForm(true)}>Raise dispute</button>}
      </div>}
      {!incoming && proposal.status === 'PENDING' && <span className="friend-status">Waiting for approval</span>}
      {showDisputeForm && <form className="friend-expense-edit-form" onSubmit={submitDispute}>
        <label htmlFor={`dispute-reason-${proposal.id}`}>Why are you disputing this expense?</label>
        <textarea id={`dispute-reason-${proposal.id}`} value={reason} maxLength={500} onChange={(event) => setReason(event.target.value)} required />
        <div><button className="small-button" type="submit" disabled={working || !reason.trim()}>Send dispute</button>
          <button className="quiet-button" type="button" onClick={() => setShowDisputeForm(false)}>Cancel</button></div>
      </form>}
      {!incoming && proposal.status === 'DISPUTED' && <form className="friend-expense-edit-form" onSubmit={submitRevision}>
        <label htmlFor={`revise-amount-${proposal.id}`}>Amount</label>
        <input id={`revise-amount-${proposal.id}`} type="number" min="0.01" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} required />
        <label htmlFor={`revise-description-${proposal.id}`}>Description</label>
        <input id={`revise-description-${proposal.id}`} value={description} maxLength={500} onChange={(event) => setDescription(event.target.value)} required />
        <label htmlFor={`revise-direction-${proposal.id}`}>Who owes?</label>
        <select id={`revise-direction-${proposal.id}`} value={direction} onChange={(event) => setDirection(event.target.value as FriendExpenseProposal['debtDirection'])}>
          <option value="PERSON_OWES_USER">They owe me</option><option value="USER_OWES_PERSON">I owe them</option>
        </select>
        <button className="small-button" type="submit" disabled={working}>{working ? 'Sending…' : 'Edit and resubmit'}</button>
      </form>}
    </li>
  )
}

function message(cause: unknown) {
  return cause instanceof Error ? cause.message : 'Could not load friend expense requests.'
}
