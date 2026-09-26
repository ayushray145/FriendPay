import { useEffect, useState, type FormEvent } from 'react'
import { getFriends, type Friend } from '../services/friends'
import {
  addGroupMember,
  createGroup,
  createGroupExpense,
  deleteGroup,
  getGroupBalances,
  getGroupDisputes,
  getGroupExpenses,
  getGroups,
  raiseGroupDispute,
  removeGroupMember,
  resolveGroupDispute,
  type GroupDispute,
  type GroupExpense,
  type GroupBalance,
  type LedgerGroup,
} from '../services/groups'

const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 })

export default function GroupsPanel({ currentUserId }: { currentUserId: string }) {
  const [groups, setGroups] = useState<LedgerGroup[]>([])
  const [groupId, setGroupId] = useState('')
  const [group, setGroup] = useState<LedgerGroup | null>(null)
  const [expenses, setExpenses] = useState<GroupExpense[]>([])
  const [balances, setBalances] = useState<GroupBalance[]>([])
  const [disputes, setDisputes] = useState<GroupDispute[]>([])
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [reloadKey, setReloadKey] = useState(0)
  const [groupName, setGroupName] = useState('')
  const [memberEmail, setMemberEmail] = useState('')
  const [friends, setFriends] = useState<Friend[]>([])
  const [friendsLoaded, setFriendsLoaded] = useState(false)
  const [friendsLoading, setFriendsLoading] = useState(false)
  const [friendsError, setFriendsError] = useState('')
  const [friendToAdd, setFriendToAdd] = useState('')
  const [showAddMembers, setShowAddMembers] = useState(false)
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [participantUserIds, setParticipantUserIds] = useState<string[] | null>(null)
  const [paidByUserId, setPaidByUserId] = useState(currentUserId)
  const [issueType, setIssueType] = useState<GroupDispute['issueType']>('WRONGLY_ADDED')
  const [expenseId, setExpenseId] = useState('')

  useEffect(() => {
    let active = true
    getGroups()
      .then((items) => {
        if (!active) return
        setGroups(items)
        if (groupId && !items.some((item) => item.id === groupId)) setGroupId('')
        setError('')
      })
      .catch((cause: unknown) => { if (active) setError(message(cause)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [reloadKey])

  useEffect(() => {
    if (!groupId) {
      setGroup(null)
      setExpenses([])
      setBalances([])
      setDisputes([])
      return
    }
    let active = true
    setLoading(true)
    Promise.all([
      fetch(`/api/v1/groups/${groupId}`, { credentials: 'include' }).then(readResponse<LedgerGroup>),
      getGroupExpenses(groupId),
      getGroupBalances(groupId),
      getGroupDisputes(groupId),
    ])
      .then(([details, groupExpenses, groupBalances, groupDisputes]) => {
        if (!active) return
        setGroup(details)
        setExpenses(groupExpenses)
        setBalances(groupBalances)
        setDisputes(groupDisputes)
        setError('')
      })
      .catch((cause: unknown) => { if (active) setError(message(cause)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [groupId, reloadKey])

  async function submit(action: () => Promise<unknown>, success: string) {
    setWorking(true)
    setError('')
    setNotice('')
    try {
      await action()
      setNotice(success)
      setReloadKey((current) => current + 1)
    } catch (cause) {
      setError(message(cause))
    } finally {
      setWorking(false)
    }
  }

  async function onCreateGroup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setWorking(true)
    setError('')
    try {
      const created = await createGroup(groupName.trim())
      setGroups((current) => [...current, created].sort((left, right) => left.name.localeCompare(right.name)))
      setGroupId(created.id)
      setGroupName('')
      setNotice('Group created.')
    } catch (cause) {
      setError(message(cause))
    } finally {
      setWorking(false)
    }
  }

  async function toggleAddMembers() {
    const opening = !showAddMembers
    setShowAddMembers(opening)
    if (!opening || friendsLoaded) return
    setFriendsLoading(true)
    setFriendsError('')
    try {
      const savedFriends = await getFriends()
      setFriends(savedFriends)
      setFriendsLoaded(true)
      setFriendToAdd(savedFriends.find((friend) => !group?.members.some((member) => member.userId === friend.userId))?.userId ?? '')
    } catch (cause) {
      setFriendsError(message(cause))
    } finally {
      setFriendsLoading(false)
    }
  }

  async function onAddFriend() {
    const selectedFriend = friends.find((friend) => friend.userId === friendToAdd)
    if (!group || !selectedFriend || group.members.some((member) => member.userId === selectedFriend.userId)) return
    setFriendToAdd('')
    await submit(() => addGroupMember(group.id, selectedFriend.email), `${selectedFriend.nickname} added to the group.`)
  }

  function toggleGroup(id: string) {
    setGroupId((current) => current === id ? '' : id)
    setShowAddMembers(false)
    setFriendToAdd('')
  }

  async function onDeleteGroup() {
    if (!group || !window.confirm(`Delete “${group.name}” and permanently remove all of its members, expenses, splits, and disputes? This cannot be undone.`)) return
    setWorking(true)
    setError('')
    setNotice('')
    try {
      await deleteGroup(group.id)
      setGroups((current) => current.filter((item) => item.id !== group.id))
      setGroupId('')
      setGroup(null)
      setExpenses([])
      setBalances([])
      setDisputes([])
      setNotice(`“${group.name}” and its data were deleted.`)
      setReloadKey((current) => current + 1)
    } catch (cause) {
      setError(message(cause))
    } finally {
      setWorking(false)
    }
  }

  const owner = group?.members.some((member) => member.userId === currentUserId && member.role === 'OWNER') ?? false
  const availableFriends = friends.filter((friend) => !group?.members.some((member) => member.userId === friend.userId))

  return (
    <section className="groups-panel" id="groups" aria-labelledby="groups-title">
      <div className="groups-heading">
        <div><p className="eyebrow">Shared space</p><h2 id="groups-title">Groups</h2><p className="muted">Keep shared plans and costs in one place.</p></div>
        <span className="count-pill">{groups.length} {groups.length === 1 ? 'group' : 'groups'}</span>
      </div>

      {error && <p className="groups-feedback error" role="alert">{error}</p>}
      {notice && <p className="groups-feedback success" role="status">{notice}</p>}

      <div className="groups-layout">
        <aside className="group-picker" aria-label="Your groups">
          {groups.map((item) => (
            <button key={item.id} className={`group-choice ${groupId === item.id ? 'selected' : ''}`} aria-expanded={groupId === item.id} onClick={() => toggleGroup(item.id)}>
              <span className="group-choice-icon" aria-hidden="true">#</span><span>{item.name}</span>
            </button>
          ))}
          {!loading && groups.length === 0 && <p className="group-picker-empty">No groups yet.</p>}
          <form className="create-group-form" onSubmit={onCreateGroup}>
            <label htmlFor="new-group-name">Create a group</label>
            <div className="inline-form"><input id="new-group-name" value={groupName} onChange={(event) => setGroupName(event.target.value)} placeholder="e.g. Goa Trip" maxLength={120} required /><button className="small-button" disabled={working}>Create</button></div>
          </form>
        </aside>

        <div className="group-details">
          {loading ? <p className="group-loading" aria-live="polite">Loading groups…</p> : group ? (
            <>
              <div className="group-title-row"><div><h3>{group.name}</h3><p className="muted">{group.members.length} members · visible to group members</p></div>
                <div className="group-title-actions">
                  {owner && <button className="quiet-button" type="button" onClick={() => void toggleAddMembers()} aria-expanded={showAddMembers}>{showAddMembers ? 'Close member options' : 'Add members'}</button>}
                  {owner && <button className="quiet-button danger-text group-delete-button" type="button" onClick={() => void onDeleteGroup()} disabled={working}>Delete group</button>}
                  <button className="quiet-button group-close-button" type="button" onClick={() => toggleGroup(group.id)}>Close details</button>
                </div>
              </div>

              {owner && showAddMembers && <section className="group-action-form" aria-label="Add group members">
                <div className="group-add-friend-row"><div><h4>Add a friend</h4><p className="form-hint">Choose an accepted friend to add directly.</p></div>
                  {friendsLoading ? <span className="group-loading">Loading friends…</span> : <div className="inline-form">
                    <select aria-label="Choose a friend" value={friendToAdd} onChange={(event) => setFriendToAdd(event.target.value)} disabled={!friends.length}>
                    <option value="">{availableFriends.length ? 'Choose a friend' : friends.length ? 'All friends are members' : 'No accepted friends'}</option>
                      {availableFriends.map((friend) =>
                        <option key={friend.userId} value={friend.userId}>{friend.nickname}</option>)}
                    </select>
                    <button className="small-button" type="button" onClick={() => void onAddFriend()} disabled={working || !availableFriends.some((friend) => friend.userId === friendToAdd)}>Add friend</button>
                  </div>}
                </div>
                {friendsError && <p className="groups-feedback error" role="alert">{friendsError}</p>}
                <form onSubmit={(event) => { event.preventDefault(); void submit(async () => { await addGroupMember(group.id, memberEmail.trim()); setMemberEmail('') }, 'Member added to the group.') }}>
                  <label htmlFor="member-email">Or add by registered email</label>
                  <div className="inline-form"><input id="member-email" type="email" value={memberEmail} onChange={(event) => setMemberEmail(event.target.value)} placeholder="Their account email" required /><button className="small-button" disabled={working}>Add by email</button></div>
                </form>
                <p className="form-hint">New members join immediately. Only you can remove them.</p>
              </section>}

              <div className="group-subsection">
                <h4>Members</h4>
                <ul className="group-members-list">
                  {group.members.map((member) => <li key={member.userId}>
                    <span className="avatar group-avatar" aria-hidden="true">{member.displayName.trim().charAt(0).toUpperCase()}</span>
                    <span className="group-member-name">{member.displayName}{member.userId === currentUserId ? ' (you)' : ''}</span>
                    {member.role === 'OWNER' ? <span className="owner-badge">Owner</span> : owner && <button className="quiet-button danger-text" disabled={working} onClick={() => void submit(() => removeGroupMember(group.id, member.userId), `${member.displayName} removed from the group.`)}>Remove</button>}
                  </li>)}
                </ul>
              </div>

              <div className="group-subsection">
                <div className="subsection-heading"><h4>Expenses</h4><span className="count-pill">{expenses.length}</span></div>
                <form className="group-expense-form" onSubmit={(event) => { event.preventDefault(); void submit(async () => { await createGroupExpense(group.id, amount, description.trim(), participantUserIds ?? group.members.map((member) => member.userId), paidByUserId); setAmount(''); setDescription(''); setParticipantUserIds(null) }, 'Expense split and added.') }}>
                  <label htmlFor="group-expense-description">Add a shared expense</label>
                  <div className="expense-inputs"><input id="group-expense-description" value={description} onChange={(event) => setDescription(event.target.value)} placeholder="What was it for?" maxLength={500} required /><input aria-label="Amount in rupees" type="number" min="0.01" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} placeholder="₹ Amount" required /><button className="small-button" disabled={working || participantUserIds?.length === 0}>Add expense</button></div>
                  <label className="expense-payer-picker">Paid by <select value={paidByUserId} onChange={(event) => setPaidByUserId(event.target.value)}>{group.members.map((member) => <option key={member.userId} value={member.userId}>{member.displayName}{member.userId === currentUserId ? ' (you)' : ''}</option>)}</select></label>
                  <fieldset className="participant-picker"><legend>Split equally between</legend>{group.members.map((member) => {
                    const activeParticipantIds = participantUserIds ?? group.members.map((item) => item.userId)
                    return <label key={member.userId}><input type="checkbox" checked={activeParticipantIds.includes(member.userId)} onChange={(event) => {
                      const next = new Set(activeParticipantIds)
                      if (event.target.checked) next.add(member.userId)
                      else next.delete(member.userId)
                      setParticipantUserIds([...next])
                    }} />{member.displayName}{member.userId === currentUserId ? ' (you)' : ''}</label>
                  })}</fieldset>
                  <p className="form-hint">The person who paid can be included or left out of the split. New expenses default to everyone.</p>
                </form>
                <div className="group-balance-list"><h5>Group balances</h5>{balances.map((balance) => <div key={balance.userId}><span>{balance.displayName}{balance.userId === currentUserId ? ' (you)' : ''}</span><strong className={balance.netBalance > 0 ? 'positive' : balance.netBalance < 0 ? 'negative' : ''}>{balance.netBalance > 0 ? 'is owed ' : balance.netBalance < 0 ? 'owes ' : ''}{money.format(Math.abs(balance.netBalance))}</strong></div>)}</div>
                {expenses.length === 0 ? <p className="group-empty">No expenses in this group yet.</p> : <ul className="group-expenses-list">{expenses.map((expense) => <li key={expense.id}><span className="expense-mark" aria-hidden="true">↗</span><span className="expense-description"><strong>{expense.description}</strong><small>Paid by {expense.paidByName} · {new Date(expense.occurredAt).toLocaleDateString()}</small><small>Split equally: {expense.shares.map((share) => `${share.displayName} ${money.format(share.shareAmount)}`).join(' · ') || 'No split saved for this older expense'}</small></span><strong className="expense-amount">{money.format(expense.amount)}</strong></li>)}</ul>}
              </div>

              <div className="group-subsection dispute-section">
                <div className="subsection-heading"><div><h4>Disputes</h4><p className="form-hint">Choose a fixed issue to flag it for the owner.</p></div><span className="count-pill">{disputes.filter((dispute) => dispute.status === 'OPEN').length} open</span></div>
                <form className="dispute-form" onSubmit={(event) => { event.preventDefault(); void submit(() => raiseGroupDispute(group.id, issueType, issueType === 'INCORRECT_AMOUNT' ? expenseId : undefined), 'Dispute raised for the owner to review.') }}>
                  <label htmlFor="dispute-issue">Raise a dispute</label>
                  <div className="expense-inputs"><select id="dispute-issue" value={issueType} onChange={(event) => setIssueType(event.target.value as GroupDispute['issueType'])}><option value="WRONGLY_ADDED">I was added by mistake</option><option value="INCORRECT_AMOUNT">An expense has the wrong amount</option></select>{issueType === 'INCORRECT_AMOUNT' && <select aria-label="Expense to dispute" value={expenseId} onChange={(event) => setExpenseId(event.target.value)} required><option value="">Choose an expense</option>{expenses.map((expense) => <option key={expense.id} value={expense.id}>{expense.description} · {money.format(expense.amount)}</option>)}</select>}<button className="small-button outline-button" disabled={working || (issueType === 'INCORRECT_AMOUNT' && expenses.length === 0)}>Raise dispute</button></div>
                </form>
                {disputes.length > 0 && <ul className="disputes-list">{disputes.map((dispute) => <li key={dispute.id}><span className={`dispute-state ${dispute.status.toLowerCase()}`}>{dispute.status === 'OPEN' ? 'Open' : 'Resolved'}</span><span className="dispute-copy"><strong>{dispute.issueType === 'WRONGLY_ADDED' ? 'Added by mistake' : 'Incorrect expense amount'}</strong><small>{dispute.issueType === 'INCORRECT_AMOUNT' ? dispute.groupExpenseDescription : `Raised by ${dispute.raisedByName}`}</small></span>{owner && dispute.status === 'OPEN' && <button className="quiet-button" disabled={working} onClick={() => void submit(() => resolveGroupDispute(group.id, dispute.id), 'Dispute marked resolved.')}>Resolve</button>}</li>)}</ul>}
              </div>
            </>
          ) : <p className="group-loading">{groups.length === 0 ? 'Create a group to get started.' : 'Choose a group to see its details.'}</p>}
        </div>
      </div>
    </section>
  )
}

async function readResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string } | null
    throw new Error(body?.message || `Request failed (${response.status})`)
  }
  return response.json() as Promise<T>
}

function message(cause: unknown) {
  return cause instanceof Error ? cause.message : 'Something went wrong. Please try again.'
}
