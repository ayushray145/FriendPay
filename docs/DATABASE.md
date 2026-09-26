# Database Design

## Finalized MVP Model

The first persistence milestone uses account-owned contacts and a shared ledger model. A contact does not need an application account; it can optionally be linked to one later. Each account's financial records remain owned by that account. This supports quick debt tracking for friends who have not joined the application and avoids trusting a client-supplied user ID.

### Tables and relationships

- `app_users`: one row per application account, identified by the unique pair `(auth_provider, auth_provider_subject)`. No password or OAuth secret is stored.
- `payment_profiles`: an optional UPI ID for one application account and a `shared_with_friends` opt-in flag (false by default). Only the owner can view, change, or remove the ID. Accepted friends can use it to create a payment link only when the owner opts in; the raw ID is not included in friend responses.
- `people`: contacts owned by an `app_user`, with a display name, optional private phone number, and optional link to another `app_user`. A phone-only contact uses its normalized phone number as its display name. Phone data does not authenticate or discover an account. Contacts and ledgers are private to their owner. Accepting a friend request creates a linked contact for each account, with each account keeping its own nickname and ledger.
- `friend_requests`: one row per request or accepted friendship, with requester, recipient, `PENDING`/`ACCEPTED`/`DECLINED` state, and each account's independent nickname for the other. A canonical user pair has at most one pending request or accepted friendship. Only the recipient can accept or decline a pending request; only either account can view or edit its own nickname. Friendship does not grant access to the other account's ledger.
- `friend_expense_proposals`: a shared friend expense awaiting recipient approval, including sender, recipient, accepted friendship, positive amount, description, debt direction, status (`PENDING`, `DISPUTED`, `APPROVED`), and dispute/approval timestamps. Pending and disputed proposals are excluded from balances. On approval, matching private `expenses` records are written for both accounts in one transaction, with reciprocal directions.
- `friend_settlements`: one shared settlement event between accepted friends. It is included in both accounts' private ledger calculations and history, preventing either side from recording a second independent settlement for the same payment.
- `friend_settlement_reports`: payer-submitted reports for external payments to an accepted friend. A report stores payer, recipient, amount, optional UPI transaction reference, status (`PENDING`, `APPROVED`, `REJECTED`), and timestamps. A partial unique index allows at most one pending report for a payer/recipient pair. Pending and rejected reports do not affect balances; approving a pending report creates the `friend_settlements` event in the same transaction.
- `expenses`: an expense belongs to the account that recorded it and to one of that account's contacts. It stores a positive `NUMERIC(19, 2)` amount, description, and direction (`PERSON_OWES_USER` or `USER_OWES_PERSON`). The direction records who paid/owes for this bilateral event.
- `settlements`: a payment record belongs to the account that recorded it and to one of its contacts. It stores a positive amount and the direction of payment, so balance calculation can subtract it from the matching debt direction while preserving history.
- `ledger_groups`: a shared group owned by the account that created it.
- `group_members`: the owner or an account directly added by the owner, with `ACTIVE` or `REMOVED` status. Added accounts become active immediately; only the owner manages membership.
- `group_expenses`: a shared group expense with amount, description, payer, recorder, and occurrence time. Only active members can read or create these records.
- `group_expense_splits`: one saved share per participating account and group expense. Shares sum exactly to the expense amount; any leftover pennies go to participants in stable user-ID order. The payer may be excluded from participants.
- `group_disputes`: an issue raised by an active member using one of the supported issue types. `INCORRECT_AMOUNT` references an expense in the same group; `WRONGLY_ADDED` references the member's group membership. The owner reviews and resolves disputes.

An account has many contacts; a contact can have many expenses and settlements. Expenses and settlements are immutable historical event records at the persistence layer for this phase. Disputed friend proposals may be edited only by the sender and must be resubmitted for approval; approved expense records remain historical events. Do not hard-delete ledger events; future corrections should use explicit reversal events. Group expenses are separately shared only among active group members; they do not expose or modify a member's private person-to-person ledger.

UPI payment links and QR codes are generated from the authenticated user's own payment profile. A payment link for an accepted friend is available only when that friend has opted in to sharing their UPI ID. Generating or opening a payment link does not add a settlement. For friends, the payer reports completion and the recipient must approve before the shared settlement is recorded. This is recipient attestation, not bank/provider verification.

### Balance rule

For a contact, compute `PERSON_OWES_USER` expense amounts minus settlements paid by the person, and subtract `USER_OWES_PERSON` expense amounts minus settlements paid by the user. Positive net means the person owes the account owner. Zero and negative transaction amounts are rejected. Store each amount as decimal with two fractional digits; Java uses `BigDecimal` and no floating-point arithmetic.

For linked accepted friends, approved proposals contribute reciprocal expenses to both account-owned contacts. Shared friend settlements reduce the same outstanding balance in both accounts' ledger views. Proposals that are pending or disputed contribute nothing until approved.

### Future phases

Group creation, owner-managed membership, shared group expense history, and disputes are implemented in Phase 8. Phase 9 stores each selected participant's equal share and derives group balances as amount paid minus shares assigned. Only active group members can read balances. Existing group expenses recorded before Phase 9 have no participant snapshot; they remain visible in expense history but are excluded from group balances. The account-owned person ledger remains private; linking a contact to a registered account does not grant access to either account's private records.

The database must represent the financial domain accurately.

The exact schema should be finalized by the agent during Phase 0 after analyzing these requirements.

Potential entities include:

- User
- Friendship / Person relationship
- Group
- GroupMember
- Expense
- ExpenseParticipant / ExpenseSplit
- Settlement
- PaymentProfile

Do not implement the schema blindly.

---

# User

A user represents an authenticated application user.

Potential information:

- id
- name
- email
- created timestamp
- updated timestamp

Authentication identity should be associated with the external identity provider rather than storing the user's password.

For the initial Google OIDC implementation, store the provider's stable subject identifier (sub) as the external identity identifier.

Recommended additional fields:

authProvider
authProviderSubject

Constraints:

authProvider + authProviderSubject must be unique.
Email should have an appropriate uniqueness constraint if the application treats it as unique.
Never store the user's Google password.
Never store OAuth client secrets in the database.

Do not store plaintext passwords.

---

# People / Relationships

Users should be able to maintain relationships with other users.

The system must support:

- adding a person
- viewing a person
- viewing the current net balance
- viewing transaction history

Consider whether a separate friendship/contact entity is necessary.

---

# Expense

An expense represents a financial event.

It should contain enough information to determine:

- amount
- payer
- participants
- description
- date/time
- creator
- group if applicable

---

# Expense Splits

For group expenses, the system needs to represent each participant's share.

Initially support equal splitting.

Later support:

- custom amounts
- percentages
- shares

---

# Settlements

A settlement represents money being paid toward an outstanding balance.

It must support:

- partial settlement
- full settlement
- settlement date
- payer
- recipient
- amount

Historical settlement information must remain available.

---

# Balance Calculation

The balance should be derived from the underlying financial transactions.

Do not maintain multiple independent sources of truth.

Example:

Expense:
₹600

Rahul owes Ayush:
₹600

Settlement:
₹200

Remaining:
₹400

---

# Important Edge Cases

The database/domain model must handle:

- multiple expenses between two users
- debt in both directions
- partial settlements
- full settlements
- multiple settlements
- zero-value expenses being rejected
- negative amounts being rejected
- deleting/editing expenses
- group expenses
- rounding
- concurrent operations
- duplicate requests where relevant

---

# Financial Precision

Use PostgreSQL NUMERIC/DECIMAL.

Use Java BigDecimal.

Define consistent rounding rules.

Do not use floating-point arithmetic.

---

# Data Integrity

Use:

- foreign keys
- unique constraints where appropriate
- not-null constraints where appropriate
- transactions
- indexes for common queries

Do not add indexes blindly.

Indexes should correspond to actual query patterns.

---

# Important Design Requirement

Before implementing the database:

1. Analyze the domain.
2. Determine whether the above entities are sufficient.
3. Identify relationships.
4. Identify cardinalities.
5. Identify what represents a debt.
6. Identify what represents a payment.
7. Determine how balances are calculated.
8. Determine how groups generate individual debts.
9. Determine how edits/deletions affect history.

Document the final schema before implementing it.
