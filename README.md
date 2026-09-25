# Split Ledger

A lightweight personal debt, expense tracking, and settlement application. The backend now includes its database/domain foundation, Google OIDC authentication, People and expense APIs, a dashboard, and shared groups; mobile implementation is planned for a later phase.

## Requirements

- Java 25
- Maven 3.6.3 or later
- Node.js 20.19+ (or 22.12+) and npm
- Docker Desktop with Docker Compose, for local PostgreSQL

## Run locally on Windows PowerShell

Install Maven if needed:

```powershell
winget install --id Apache.Maven --exact
winget install --id Docker.DockerDesktop --exact
```

Restart PowerShell after installing Maven, and start Docker Desktop before starting PostgreSQL.

Open three PowerShell terminals from the repository root.

**Terminal 1 — PostgreSQL** (Docker Desktop must be running):

```powershell
docker compose up -d postgres
```

**Terminal 2 — backend:**

```powershell
cd backend
mvn spring-boot:run
```

The API starts at `http://localhost:8080`. Check it at `http://localhost:8080/api/v1/health`.

**Terminal 3 — frontend:**

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

Open the Vite URL shown in the terminal, usually `http://localhost:5173`. The Vite development server proxies `/api` requests to the backend on port 8080. The Google sign-in button starts OAuth directly on the backend; `VITE_BACKEND_URL` can override its origin (see `frontend/.env.example`).

Yes: during development, run the frontend and backend in separate terminals. PostgreSQL runs as a third service through Docker Compose. Stop the database with `docker compose down`; its data remains in a Docker volume. To remove the local database data too, run `docker compose down --volumes`.

The backend automatically loads `backend/.env` when started with `backend` as the working directory. It is ignored by Git. Copy `backend/.env.example` as a starting point if needed, then fill in local values. OS environment variables override values from `.env`.

### Google sign-in (Phase 3)

Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in `backend/.env` to enable Google OIDC login. In Google Cloud Console, add this authorized redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
```

With the frontend running, click **Continue with Google**. For a direct backend check, navigate to `http://localhost:8080/oauth2/authorization/google`. The backend creates or retrieves the application account and redirects to `APP_FRONTEND_URL` after successful login. The authenticated session is held by the backend; the OAuth client secret stays on the backend. Without both Google variables, health checks remain available and other API routes still require authentication, but Google login is not enabled.

The backend exposes `GET /api/v1/auth/me` for the signed-in application user and `GET /api/v1/auth/csrf` for a CSRF token. Send the returned token using its `headerName` for `POST /api/v1/auth/logout` and future state-changing requests.

### People API (Phase 4)

All People endpoints require an authenticated session. Create a contact with `POST /api/v1/people` and a JSON body such as `{"displayName":"Rahul"}`. Use `GET /api/v1/people` to list the signed-in user's contacts and `GET /api/v1/people/{personId}` to read one. A contact belongs to the signed-in account; supplying another account's contact ID returns `404`.

### Friend requests

In the web app, open **People**, enter a registered user's email, optionally choose your private nickname for them, and send a request. The recipient sees it in their People screen and must accept it. Each account gets its own linked private ledger contact after acceptance; each can edit their nickname independently. Friendship does not expose either person's ledger. API routes are `POST /api/v1/friends/requests`, `GET /api/v1/friends/requests`, `POST /api/v1/friends/requests/{requestId}/accept`, `POST /api/v1/friends/requests/{requestId}/decline`, and `GET /api/v1/friends`.

### Debt ledger API (Phases 5 and 7)

Create an expense for an owned contact with `POST /api/v1/people/{personId}/expenses` and a body such as `{"amount":600.00,"description":"Dinner"}`. The default direction is `PERSON_OWES_USER`; pass `"debtDirection":"USER_OWES_PERSON"` when you owe the contact instead. Amounts must be positive and have at most two decimal places. Read expense history at `GET /api/v1/people/{personId}/expenses`, an individual expense at `GET /api/v1/people/{personId}/expenses/{expenseId}`, and the net balance at `GET /api/v1/people/{personId}/balance`. A positive balance means the person owes the signed-in user; a negative balance means the signed-in user owes the person.

Record a partial or full settlement with `POST /api/v1/people/{personId}/settlements`, sending an amount and `paymentDirection` (`PERSON_OWES_USER` when the person paid you, `USER_OWES_PERSON` when you paid them). A settlement cannot exceed the remaining debt in that direction; the API returns `409 Conflict` if it does. View settlement history at `GET /api/v1/people/{personId}/settlements` or combined chronological expense and settlement activity at `GET /api/v1/people/{personId}/ledger`. Settlements update both the per-person balance and dashboard totals while remaining in history.

### Dashboard API (Phase 6)

`GET /api/v1/dashboard` returns the signed-in user's totals (`totalOwedToYou`, `totalYouOwe`, and `netBalance`) after recorded settlements and nonzero per-person balances. Positive per-person balances mean that person owes the user; negative balances mean the user owes that person. Contacts with no expenses or a zero net balance are omitted.

### Groups API (Phase 8)

Create and list groups at `POST` and `GET /api/v1/groups`. A newly created group contains its sole owner. The owner adds an existing account immediately by email with `POST /api/v1/groups/{groupId}/members` and removes a member with `DELETE /api/v1/groups/{groupId}/members/{userId}`. Added members cannot accept invitations or change membership. Active members can read shared expenses and add a group expense; only the owner can see every dispute or resolve one. Members can raise one of two hardcoded disputes with `POST /api/v1/groups/{groupId}/disputes`: `WRONGLY_ADDED` (no expense ID) or `INCORRECT_AMOUNT` (with a `groupExpenseId`). Personal ledgers remain private to each account. Equal-split shares and per-member balances are Phase 9.

## Build the frontend

```powershell
cd frontend
npm.cmd run build
```

## Repository guide

- `backend/` — Spring Boot REST API
- `frontend/` — React and TypeScript web app
- `mobile/` — reserved for the later React Native phase
- `docs/` — product, architecture, database, and roadmap documents
- `AGENTS.md` — repository development rules
