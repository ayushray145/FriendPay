import { useEffect, useState } from 'react'
import { ApiError, getCurrentUser, getDashboard, type CurrentUser, type DashboardData } from './services/dashboard'
import { ApiRequestError } from './services/api'
import ExpenseWorkspace from './components/ExpenseWorkspace'
import FriendsPanel from './components/FriendsPanel'
import GroupsPanel from './components/GroupsPanel'
import PaymentsPanel from './components/PaymentsPanel'

type LoadState = 'loading' | 'ready' | 'signed-out' | 'error'
type AppView = 'overview' | 'people' | 'expenses' | 'groups' | 'payments'

const currency = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 })
const googleLoginUrl = `${import.meta.env.VITE_BACKEND_URL ?? (import.meta.env.DEV ? 'http://localhost:8080' : '')}/oauth2/authorization/google`

export default function App() {
  const [state, setState] = useState<LoadState>('loading')
  const [dashboard, setDashboard] = useState<DashboardData | null>(null)
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [activeView, setActiveView] = useState<AppView>('overview')

  useEffect(() => {
    let active = true
    Promise.all([getCurrentUser(), getDashboard()])
      .then(([currentUser, data]) => {
        if (!active) return
        setUser(currentUser)
        setDashboard(data)
        setState('ready')
      })
      .catch((error: unknown) => {
        if (!active) return
        if ((error instanceof ApiError || error instanceof ApiRequestError) && error.status === 401) {
          setState('signed-out')
        } else {
          setState('error')
        }
      })
    return () => { active = false }
  }, [reloadKey])

  if (state === 'loading') {
    return <main className="center-state" aria-live="polite"><span className="loader" />Loading your ledger…</main>
  }

  if (state === 'signed-out') {
    return (
      <main className="center-state">
        <section className="auth-card">
          <Brand />
          <p className="eyebrow">Your money, in good company</p>
          <h1>Keep every shared expense clear.</h1>
          <p className="muted">Sign in to see your people, balances, and what needs settling.</p>
          <a className="primary-button" href={googleLoginUrl}>Continue with Google <span aria-hidden="true">↗</span></a>
          <p className="fine-print">Your financial information is private to your account.</p>
        </section>
      </main>
    )
  }

  if (state === 'error' || !dashboard) {
    return (
      <main className="center-state">
        <section className="message-card" role="alert">
          <Brand />
          <h1>We couldn’t load your dashboard.</h1>
          <p className="muted">Check that the backend is running, then try again.</p>
          <button className="primary-button" onClick={() => window.location.reload()}>Try again</button>
        </section>
      </main>
    )
  }

  const firstName = user?.displayName?.trim().split(/\s+/)[0] || 'there'
  const navigation = <>
    <button className={`nav-link ${activeView === 'people' ? 'active' : ''}`} onClick={() => setActiveView('people')}><span>♧</span> People</button>
    <button className={`nav-link ${activeView === 'expenses' ? 'active' : ''}`} onClick={() => setActiveView('expenses')}><span>+</span> Add expense</button>
    <button className={`nav-link ${activeView === 'groups' ? 'active' : ''}`} onClick={() => setActiveView('groups')}><span>#</span> Groups</button>
    <button className={`nav-link ${activeView === 'payments' ? 'active' : ''}`} onClick={() => setActiveView('payments')}><span>₹</span> UPI payments</button>
  </>

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <Brand />
        <nav aria-label="Main navigation">
          <button className={`nav-link ${activeView === 'overview' ? 'active' : ''}`} aria-current={activeView === 'overview' ? 'page' : undefined} onClick={() => setActiveView('overview')}><span>◫</span> Overview</button>
          {navigation}
        </nav>
        <div className="sidebar-bottom">
          <div className="avatar" aria-hidden="true">{firstName.charAt(0).toUpperCase()}</div>
          <div className="user-summary"><strong>{user?.displayName}</strong><span>{user?.email}</span></div>
        </div>
      </aside>

      <nav className="mobile-nav" aria-label="Main navigation">
        <button className={`nav-link ${activeView === 'overview' ? 'active' : ''}`} aria-current={activeView === 'overview' ? 'page' : undefined} onClick={() => setActiveView('overview')}><span>◫</span>Overview</button>
        <button className={`nav-link ${activeView === 'people' ? 'active' : ''}`} aria-current={activeView === 'people' ? 'page' : undefined} onClick={() => setActiveView('people')}><span>♧</span>People</button>
        <button className={`nav-link ${activeView === 'expenses' ? 'active' : ''}`} aria-current={activeView === 'expenses' ? 'page' : undefined} onClick={() => setActiveView('expenses')}><span>+</span>Expense</button>
        <button className={`nav-link ${activeView === 'groups' ? 'active' : ''}`} aria-current={activeView === 'groups' ? 'page' : undefined} onClick={() => setActiveView('groups')}><span>#</span>Groups</button>
        <button className={`nav-link ${activeView === 'payments' ? 'active' : ''}`} aria-current={activeView === 'payments' ? 'page' : undefined} onClick={() => setActiveView('payments')}><span>₹</span>UPI</button>
      </nav>

      <section className="main-content">
        <header className="topbar"><span className="mobile-brand"><Brand /></span><span className="private-label"><span className="shield">✓</span> Personal ledger stays private</span></header>
        <div className="content-wrap">
          {activeView === 'overview' && <>
            <section className="welcome-row">
              <div>
                <p className="eyebrow">Your overview</p>
                <h1>Good day, {firstName}<span className="wave">.</span></h1>
                <p className="muted">A quick summary of your shared expenses.</p>
              </div>
              <div className="date-chip"><span aria-hidden="true">◷</span> Live ledger</div>
            </section>
            <section className="summary-grid" aria-label="Balance summary">
              <SummaryCard label="You are owed" amount={dashboard.totalOwedToYou} icon="↙" tone="green" />
              <SummaryCard label="You owe" amount={dashboard.totalYouOwe} icon="↗" tone="amber" />
              <SummaryCard label="Net balance" amount={dashboard.netBalance} icon="＝" tone="dark" />
            </section>
            <section className="overview-shortcuts" aria-label="Quick actions">
              <div><h2>Connect with your friends</h2><p className="muted">Send a request to a registered Split Ledger account.</p></div>
              <button className="small-button" onClick={() => setActiveView('people')}>Add a friend <span aria-hidden="true">→</span></button>
            </section>
          </>}

          {activeView === 'people' && <>
            <section className="welcome-row page-welcome"><div><p className="eyebrow">Your contacts</p><h1>People</h1><p className="muted">Connect with other registered Split Ledger users.</p></div></section>
            <FriendsPanel />
          </>}

          {activeView === 'expenses' && <ExpenseWorkspace onExpenseCreated={() => setReloadKey((current) => current + 1)} />}
          {activeView === 'groups' && <GroupsPanel currentUserId={user?.id ?? ''} />}
          {activeView === 'payments' && <PaymentsPanel suggestedPayment={null} />}
          <footer className="page-footer">Personal balances are private. Shared group expenses are visible to active group members.</footer>
        </div>
      </section>
    </main>
  )
}

function Brand() {
  return <div className="brand"><span className="brand-mark">s</span><span>split<span className="brand-light">ledger</span></span></div>
}

function SummaryCard({ label, amount, icon, tone }: { label: string; amount: number; icon: string; tone: string }) {
  return (
    <article className={`summary-card ${tone}`}>
      <div className="summary-top"><span>{label}</span><span className="summary-icon" aria-hidden="true">{icon}</span></div>
      <strong>{currency.format(amount)}</strong>
      <span className="summary-caption">Across your people</span>
    </article>
  )
}
