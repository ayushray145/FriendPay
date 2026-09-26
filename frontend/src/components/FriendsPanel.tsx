import { useEffect, useState, type FormEvent } from 'react'
import {
  acceptFriendRequest, declineFriendRequest, getFriendRequests, getFriends,
  sendFriendRequest, updateFriendNickname,
  type Friend, type FriendRequest,
} from '../services/friends'

export default function FriendsPanel() {
  const [friends, setFriends] = useState<Friend[]>([])
  const [requests, setRequests] = useState<FriendRequest[]>([])
  const [email, setEmail] = useState('')
  const [nickname, setNickname] = useState('')
  const [editingFriend, setEditingFriend] = useState<string | null>(null)
  const [editNickname, setEditNickname] = useState('')
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [showRequests, setShowRequests] = useState(false)

  async function refresh() {
    setLoading(true)
    setError('')
    try {
      const [friendList, friendRequests] = await Promise.all([getFriends(), getFriendRequests()])
      setFriends(friendList)
      setRequests(friendRequests)
    } catch (cause) {
      setError(message(cause, 'Could not load friends. Please try again.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void refresh() }, [])

  async function onSend(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setWorking(true)
    setError('')
    setNotice('')
    try {
      const sent = await sendFriendRequest(email.trim(), nickname)
      setRequests((current) => [sent, ...current])
      setEmail('')
      setNickname('')
      setNotice(`Friend request sent to ${sent.email}.`)
    } catch (cause) {
      setError(message(cause, 'Could not send friend request. Please try again.'))
    } finally {
      setWorking(false)
    }
  }

  async function onAccept(request: FriendRequest) {
    setWorking(true)
    setError('')
    try {
      const friend = await acceptFriendRequest(request.id)
      setFriends((current) => [...current, friend].sort((a, b) => a.nickname.localeCompare(b.nickname)))
      setRequests((current) => current.filter((item) => item.id !== request.id))
      setNotice(`You and ${friend.nickname} are now friends.`)
    } catch (cause) {
      setError(message(cause, 'Could not accept the friend request.'))
    } finally {
      setWorking(false)
    }
  }

  async function onDecline(request: FriendRequest) {
    setWorking(true)
    setError('')
    try {
      await declineFriendRequest(request.id)
      setRequests((current) => current.filter((item) => item.id !== request.id))
    } catch (cause) {
      setError(message(cause, 'Could not decline the friend request.'))
    } finally {
      setWorking(false)
    }
  }

  async function onSaveNickname(friend: Friend) {
    setWorking(true)
    setError('')
    try {
      const updated = await updateFriendNickname(friend.requestId, editNickname.trim())
      setFriends((current) => current.map((item) => item.requestId === friend.requestId ? updated : item))
      setEditingFriend(null)
    } catch (cause) {
      setError(message(cause, 'Could not update nickname.'))
    } finally {
      setWorking(false)
    }
  }

  return (
    <div className="friends-page">
      <section className="welcome-row people-page-heading">
        <div><p className="eyebrow">Your contacts</p><h1>People</h1><p className="muted">Find friends by registered email and choose your private nickname.</p></div>
        <button className={`request-toggle${requests.length ? ' has-pending' : ''}`} type="button"
          aria-expanded={showRequests} aria-controls="friend-requests-panel" onClick={() => setShowRequests((shown) => !shown)}>
          <span className="request-toggle-label">Friend requests</span><span className="count-pill">{requests.length}</span>
        </button>
      </section>
      <section className="friend-add-card" aria-labelledby="add-friend-title">
        <div>
          <p className="eyebrow">Connect with people</p>
          <h2 id="add-friend-title">Add a friend</h2>
          <p className="muted">Send a request to someone who already has a Split Ledger account.</p>
        </div>
        <form className="friend-add-form" onSubmit={(event) => void onSend(event)}>
          <label>Email address<input type="email" autoComplete="email" maxLength={320} value={email}
            onChange={(event) => setEmail(event.target.value)} placeholder="friend@example.com" required /></label>
          <label><span className="friend-label-title">Nickname <span>(only you can see this)</span></span><input maxLength={80} value={nickname}
            onChange={(event) => setNickname(event.target.value)} placeholder="e.g. Rahul" required /></label>
          <button className="small-button" disabled={working}>{working ? 'Sending…' : 'Send friend request'}</button>
        </form>
        {error && <p className="payment-feedback error" role="alert">{error}</p>}
        {notice && <p className="payment-feedback success" role="status">{notice}</p>}
      </section>

      <section className="friend-list-card requests-panel" id="friend-requests-panel" aria-labelledby="friend-requests-title" hidden={!showRequests}>
        <div className="panel-heading"><div><h2 id="friend-requests-title">Friend requests</h2><p className="muted">Accept requests before someone appears as a friend.</p></div>
          <span className="count-pill">{requests.length}</span></div>
        {loading ? <p className="friend-state" aria-live="polite">Loading friend requests…</p> : requests.length === 0 ?
          <p className="friend-state">No pending friend requests.</p> : <ul className="friend-items">
            {requests.map((request) => <li className="friend-item" key={request.id}>
              <div className="friend-identity"><strong>{request.displayName}</strong><span>{request.email}</span>
                <small>{request.direction === 'INCOMING' ? 'Wants to connect with you' : 'Request sent'}</small></div>
              {request.direction === 'INCOMING' ? <div className="friend-actions">
                <button className="small-button" disabled={working} onClick={() => void onAccept(request)}>Accept</button>
                <button className="quiet-button" disabled={working} onClick={() => void onDecline(request)}>Decline</button>
              </div> : <span className="friend-status">Pending</span>}
            </li>)}
          </ul>}
      </section>

      <section className="friend-list-card friends-subgroup" aria-labelledby="friends-title">
        <div className="panel-heading"><div><p className="eyebrow">People</p><h2 id="friends-title">Friends</h2><p className="muted">Accepted connections. Nicknames are private and ledgers remain private.</p></div>
          <span className="count-pill">{friends.length}</span></div>
        {loading ? <p className="friend-state" aria-live="polite">Loading friends…</p> : friends.length === 0 ?
          <p className="friend-state">Accepted friends will appear here.</p> : <ul className="friend-items">
            {friends.map((friend) => <li className="friend-item" key={friend.requestId}>
              <div className="friend-identity"><strong>{friend.nickname}</strong><span>{friend.email}</span>
                {friend.nickname !== friend.displayName && <small>Account name: {friend.displayName}</small>}</div>
              {editingFriend === friend.requestId ? <div className="friend-edit">
                <input aria-label={`Nickname for ${friend.displayName}`} maxLength={80} value={editNickname}
                  onChange={(event) => setEditNickname(event.target.value)} />
                <button className="small-button" disabled={working || !editNickname.trim()} onClick={() => void onSaveNickname(friend)}>Save</button>
                <button className="quiet-button" onClick={() => setEditingFriend(null)}>Cancel</button>
              </div> : <button className="quiet-button" onClick={() => { setEditingFriend(friend.requestId); setEditNickname(friend.nickname) }}>Edit nickname</button>}
            </li>)}
          </ul>}
      </section>
      {!loading && <button className="quiet-button friend-refresh" onClick={() => void refresh()}>Refresh requests</button>}
    </div>
  )
}

function message(cause: unknown, fallback: string) {
  return cause instanceof Error ? cause.message : fallback
}
