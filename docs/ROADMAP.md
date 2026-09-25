# Development Roadmap

The project must be developed sequentially.

Do not skip ahead unless explicitly instructed.

---

# Phase 0 — Requirements & Architecture

No major application code.

Tasks:

- understand product
- inspect repository
- finalize MVP
- finalize domain model
- finalize database model
- finalize API architecture
- finalize frontend architecture
- identify ambiguities

Deliverables:

- architecture document
- database design
- API plan
- user flows
- implementation plan

---

# Phase 1 — Project Setup

Create:

backend/
frontend/

Set up:

- Java 25
- Spring Boot
- Maven
- React
- TypeScript
- Vite
- Git
- environment configuration

Verify:

- backend starts
- frontend starts
- database connection works

---

# Phase 2 — Database & Domain

Implement:

- entities
- repositories
- migrations/schema
- relationships

Write tests.

Verify database integrity.

---

## Phase 3 — Authentication & User System

Goal: establish secure user authentication and application-level user identity.

Tasks:

* Configure Spring Security.
* Configure OAuth 2.0 / OpenID Connect.
* Integrate Google as the initial identity provider.
* Configure Google OAuth credentials through environment variables.
* Implement login flow.
* Implement logout flow.
* Create/retrieve the application's User record after successful authentication.
* Associate the authenticated provider identity with the application User.
* Protect authenticated API endpoints.
* Implement server-side authorization checks.
* Verify that users cannot access another user's financial data.
* Add authentication and authorization tests.

Security requirements:

* Do not store user passwords.
* Do not expose OAuth client secrets to frontend or mobile applications.
* Do not hardcode credentials or secrets.
* Do not trust user IDs supplied by the client for authorization.
* Use the authenticated application User from the Spring Security context.

---

# Phase 4 — People

Implement:

- add person
- list people
- person details
- relationship handling

Test complete API flow.

---

# Phase 5 — Core Debt Ledger

Implement:

- add expense
- person-to-person debt
- transaction history
- balance calculation

This is the most important phase.

Test extensively.

Examples:

- one expense
- multiple expenses
- reverse debt
- multiple debts
- zero amount
- negative amount
- rounding

---

# Phase 6 — Dashboard

Implement:

- total owed
- total owed by user
- net balance
- people balances

Use real backend data.

No hardcoded financial values.

---

# Phase 7 — Person Ledger

Implement:

- [x] detailed history
- [x] current outstanding amount
- [x] partial settlement
- [x] full settlement

Test settlement calculations thoroughly. **Phase 7 backend API is implemented; the person ledger UI remains part of Phase 11.**

---

# Phase 8 — Groups

Implement:

- [x] create group
- [x] owner adds and removes registered members directly
- [x] group details
- [x] shared group expenses
- [x] member disputes for "wrongly added" and "incorrect amount"

Group expenses initially record the total amount and payer. Phase 9 adds shared expense splits and per-member group balances. Only the owner can change membership; added users join immediately.

---

# Phase 9 — Equal Bill Splitting

Implement:

- [x] equal split with optional participant selection (defaults to all active members)
- [x] payer selection, including the payer in or excluding the payer from the split
- [x] saved per-member shares and derived group balances
- [x] web UI for entering splits, viewing shares, and viewing group balances

Group balances use the same paid-minus-share balance rule as other ledgers, derived only from shared group expenses and shares. They do not change or expose private person ledgers.

Test:

- [x] 2 people
- [x] 3 people
- [x] 5 people
- [x] rounding
- [x] payer included
- [x] payer excluded

**Phase 9 backend and basic web flow are implemented. Responsive UI polish remains in Phase 11.**

---

# Phase 10 — UPI / QR

Implement:

- [x] private UPI ID profile (view, save, remove)
- [x] UPI payment URI and locally generated QR code
- [x] payment request action for amounts owed to the signed-in user
- [x] manual partial and full settlement recording from a person's ledger

UPI requests only start payment in another app. They never confirm or record a settlement automatically.

**Phase 10 backend and basic web flow are implemented. Responsive UI polish remains in Phase 11.**

---

# Phase 11 — Responsive Web UI

Complete:

- [x] dashboard
- [x] people list and private contact creation by name or phone number
- [x] person details, balance, and transaction history
- [x] add personal expense in either debt direction
- [x] groups
- [x] settlement recording and history
- [x] UPI payment profile and payment request flow
- [x] signed-in profile summary
- [x] registered-user friend requests by email, recipient acceptance, and private nicknames
- [x] create a separate private ledger contact for each account when a friend request is accepted

Handle:

- loading
- empty
- error
- success states

**Phase 11 core web flows are implemented. Responsive device testing remains in Phase 12.**

Friend lookup by phone number is not implemented. Phone numbers remain private contact data and are not used for sign-in or account discovery.

---

# Phase 12 — Testing & Hardening

Perform:

- backend test suite
- frontend tests
- API tests
- security testing
- validation testing
- financial edge-case testing
- responsive testing

Fix all blocking issues.

---

# Phase 13 — Deployment

Deploy:

- frontend
- backend
- PostgreSQL

Configure:

- environment variables
- CORS
- production configuration
- secure secrets

Perform end-to-end testing against production.

---

# Phase 14 — Mobile Application

Create React Native application.

Reuse:

- same API
- same authentication
- same backend
- same database

Build mobile screens incrementally.

Test each screen against the real backend.

---

# Phase 15 — Mobile-Specific Features

Potential features:

- push notifications
- QR scanner
- deep links
- sharing
- biometric lock

Only implement after the basic mobile application is stable.

---

# Future Work

Potential later features:

- custom splits
- percentage splits
- recurring expenses
- reminders
- debt simplification
- OCR receipts
- analytics
- exports
- offline support
