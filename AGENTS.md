# AGENTS.md

## Project Development Rules

You are the lead software engineer for this repository.

The goal is to build the application incrementally and professionally. Do NOT vibe-code or generate the entire application at once.

## Core Rule

Never implement the entire project in one pass.

Work in small, independently verifiable milestones.

For every milestone:

1. Explain what you are going to implement.
2. Identify the files/components that will change.
3. Implement only that milestone.
4. Compile/build the affected project.
5. Run relevant tests.
6. Inspect failures and warnings.
7. Fix the problems.
8. Run the tests/build again.
9. Verify the implementation.
10. Only then proceed to the next milestone.

Never knowingly move forward with failing tests or broken compilation.

---

## Before Coding

Before starting a major phase:

1. Read PRODUCT.md.
2. Read ARCHITECTURE.md.
3. Read DATABASE.md.
4. Read ROADMAP.md.
5. Inspect the existing repository.
6. Determine what has already been implemented.
7. Do not recreate existing functionality.

Do not make major architectural changes without explaining them first.

---

## Engineering Philosophy

Prefer:

- simple solutions
- maintainable code
- clear separation of responsibilities
- strong typing
- meaningful names
- small focused classes/components
- automated tests
- real implementations

Avoid:

- unnecessary abstractions
- unnecessary dependencies
- premature optimization
- microservices
- duplicated business logic
- giant classes/components
- hardcoded application data
- fake APIs
- placeholder business logic

Do not introduce a technology merely because it is popular.

---

# Backend Rules

Backend stack:

- Java 25
- Spring Boot
- Maven
- Spring Data JPA
- Hibernate
- PostgreSQL
- Spring Security
- JWT
- JUnit
- Mockito

Use the architecture:

Controller
    ↓
Service
    ↓
Repository
    ↓
Database

Business logic belongs primarily in the service/domain layer.

Controllers should remain thin.

Do not expose JPA entities directly through REST APIs.

Use DTOs.

Validate incoming requests.

Use consistent exception handling.

---

# Financial Data Rules

Money must NEVER be represented using floating-point types such as:

- float
- double

Use:

- Java BigDecimal
- PostgreSQL NUMERIC/DECIMAL

Money calculations must be deterministic and properly rounded.

Never silently lose money due to floating-point precision.

---

# Security Rules

Passwords must never be stored in plaintext.

Use secure password hashing.

Protected endpoints must verify authentication and authorization.

Never trust user IDs supplied by the frontend when determining ownership.

Users must only be able to access data they are authorized to access.

Never commit secrets, passwords, JWT keys, API keys, or database credentials.

Use environment variables for secrets.

Do not expose stack traces or internal implementation details to users.

---

# Frontend Rules

Web stack:

- React
- TypeScript
- Vite

Keep API communication separate from UI components.

Do not hardcode balances or financial data.

All important financial information must come from the backend.

Important API-driven screens must handle:

- loading
- success
- empty state
- error state

The web application must be responsive.

---

# Mobile Rules

The mobile application will use:

- React Native
- TypeScript

The mobile app must use the existing Spring Boot API.

Do NOT create a separate backend for mobile.

Do not duplicate business logic between web and mobile.

The backend remains the source of truth.

---

# Testing Rules

Testing is mandatory.

Backend:

- unit tests
- service tests
- repository/integration tests where appropriate
- API tests

Frontend:

- component tests where useful
- API integration tests
- important user-flow tests

Always test both happy paths and failure/edge cases.

Financial calculations require especially thorough tests.

---

# Git Rules

Use small logical commits.

Prefer commits such as:

feat: add authentication
feat: add people management
feat: implement expense ledger
test: add balance calculation tests
feat: add settlements

Avoid giant commits containing unrelated changes.

---

# Agent Behavior

Do not claim that something works unless it has actually been tested.

If a test fails:

1. Diagnose the failure.
2. Fix the underlying problem.
3. Run the test again.
4. Report the result.

Do not simply suppress errors.

If you discover an important architectural ambiguity, stop and ask before making a decision that could affect the rest of the system.

For small implementation details, use reasonable engineering judgment.

---

# Current Development Rule

At the beginning of every new session:

1. Inspect the repository.
2. Read the documentation.
3. Determine the current implementation state.
4. Identify the next unfinished roadmap milestone.
5. Work only on that milestone unless instructed otherwise.

---

### Authentication rules

Authentication must use the authentication architecture defined in ARCHITECTURE.md.

Initial authentication provider:

* Google OpenID Connect

Do not implement custom password authentication unless explicitly approved.

Do not:

* store plaintext passwords
* store Google passwords
* expose OAuth client secrets to frontend code
* hardcode OAuth credentials
* trust frontend-supplied user IDs for authorization
* bypass Spring Security for protected API endpoints
* create authentication logic inside controllers

Authentication-related code must use Spring Security's established OAuth2/OIDC mechanisms.

Authorization must be enforced server-side.

Every protected resource must verify ownership or access rights using the authenticated application User.

Authentication implementation must be tested before building financial features on top of it.