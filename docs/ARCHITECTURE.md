# Architecture

## Overall Architecture

The application will use a modular monolith backend with separate client applications.

Architecture:

React Web
      ↓
Spring Boot REST API
      ↓
PostgreSQL

Later:

React Native
      ↓
Spring Boot REST API
      ↓
PostgreSQL

Both web and mobile use the same backend.

---

# Backend

Technology:

- Java 25
- Spring Boot
- Maven
- Spring Web
- Spring Data JPA
- Hibernate
- PostgreSQL
- Spring Security
- OAuth 2.0 / OpenID Connect (Google)
- server-side authenticated session cookie
- Bean Validation
- JUnit
- Mockito

Backend layers:

Controller
    ↓
Service
    ↓
Repository
    ↓
Database

---

# Controller Layer

Responsibilities:

- receive HTTP requests
- validate request DTOs
- authenticate/authorize requests
- call services
- return HTTP responses

Controllers should contain minimal business logic.

---

# Service Layer

Responsibilities:

- business rules
- balance calculations
- expense processing
- settlement logic
- group splitting
- validation involving multiple entities

Financial logic must live here rather than in controllers.

---

# Repository Layer

Use Spring Data JPA repositories.

Repositories are responsible for persistence operations.

Do not put complex business rules into repositories.

---

# DTOs

Do not expose JPA entities directly through the API.

Use:

- request DTOs
- response DTOs

This prevents API contracts from becoming tightly coupled to database entities.

---

### Authentication

The application uses OpenID Connect (OIDC) for user authentication.

Initial identity provider:

* Google

Spring Security acts as the OAuth 2.0/OIDC client.

Authentication flow:

1. User selects "Continue with Google".
2. The client initiates the authentication flow.
3. Google authenticates the user.
4. Google redirects the authentication result back through the configured backend flow.
5. Spring Security validates the OIDC authentication response and ID token.
6. The backend identifies the corresponding application User using the provider identity.
7. If the user does not exist, the backend creates the User record.
8. The backend establishes the application's authenticated session.
9. Subsequent API requests are authenticated before accessing protected resources.

The Google account is used for authentication. Application financial data remains owned and controlled by the Split Ledger backend.

### Authorization

Authentication does not automatically grant access to application data.

Every protected operation must verify that the authenticated application User owns or is authorized to access the requested resource.

For example:

* A user can access their own expenses.
* A user cannot access another user's expenses by changing an ID in an API request.
* A user can only modify settlements they are authorized to modify.

Financial/business authorization rules belong in the backend.

Groups are shared resources. The creator is the sole owner. The owner adds registered accounts directly; they become active members immediately and cannot accept, decline, or remove themselves. Only the owner can add or remove members. Active members can read shared group data and raise a fixed-type dispute; only the owner can see all group disputes and resolve them. Removed members cannot read group details, expenses, or disputes. Group authorization does not grant access to any member's private people or personal ledger.

Friend requests are different from group membership: the recipient must accept before a friendship is established. Each account can choose its own private nickname and gets its own linked ledger contact. Friend status never grants access to the other account's personal ledger.

Expenses between accepted friends use a proposal lifecycle. The sender's expense remains pending and does not affect either balance until the recipient approves it. Approval writes corresponding expense records into each account's private ledger, with reciprocal direction and the same description, amount, and date. The recipient can dispute a pending proposal; the sender edits and resubmits it for approval. Settlements recorded against an approved shared friend balance are represented once as a shared event and are reflected in both accounts' private ledger views. This shared workflow is available only for accepted friends; expenses with other contacts continue to be recorded privately by the current account.

### Security principles

* Never store Google passwords.
* Never expose OAuth client secrets to the frontend.
* Never hardcode secrets in source code.
* Store secrets in environment variables or the deployment platform's secret manager.
* Use HTTPS in production.
* Validate authentication on the backend.
* Do not trust user IDs supplied by the frontend to determine ownership.
* Derive the authenticated application user from the server-side security context.

---

# Database

PostgreSQL is the primary database.

Use relational modeling because:

- users have relationships
- expenses have participants
- groups have members
- settlements reference debts/transactions
- financial data requires consistency

Use transactions where necessary.

---

# Money

Use:

Java:
BigDecimal

PostgreSQL:
NUMERIC / DECIMAL

Never use float/double for financial calculations.

---

# Web Frontend

Use:

- React
- TypeScript
- Vite
- React Router

Suggested structure:

src/
├── components/
├── pages/
├── layouts/
├── hooks/
├── services/
├── api/
├── types/
├── utils/
├── context/
└── routes/

Avoid unnecessary state-management libraries initially.

---

# Mobile

Use:

React Native
+
TypeScript

Mobile communicates with the same backend REST API.

The mobile application should not contain duplicated financial/business logic.

---

# API

Use REST APIs.

Use:

- JSON
- meaningful HTTP status codes
- DTOs
- consistent error responses
- request validation

The API should be versionable if necessary later.

---

# Deployment

Initial deployment can use:

Frontend:
Vercel/Netlify

Backend:
Render/Railway or equivalent

Database:
Managed PostgreSQL

Secrets must be stored using environment variables.

---

# Architecture Principles

1. Keep the backend as the source of truth.
2. Keep business logic out of UI clients.
3. Keep financial calculations on the backend.
4. Avoid duplicated logic.
5. Prefer simplicity.
6. Do not introduce microservices.
7. Do not introduce Redis/Kafka/etc. without a demonstrated need.
8. Keep mobile and web clients independent from database implementation.
