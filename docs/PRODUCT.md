# Product Specification

## Product Concept

This application is a lightweight personal debt, expense and settlement application.

The core idea is not simply bill splitting.

The primary use case is:

> Quickly record that another person owes you money, keep a detailed record of why they owe you, see the total outstanding balance with that person, and allow them to settle the entire balance conveniently.

Bill splitting and groups are secondary features.

---

# Core Example

A user has 20 friends.

They go out with Rahul.

The user pays ₹600 for Rahul's dinner.

Instead of creating a group, the user should simply be able to:

1. Click "+ Add Expense"
2. Enter ₹600
3. Select Rahul
4. Enter "Dinner"
5. Save

The system records:

Rahul owes the user ₹600.

---

# Person Ledger

Each person should have a dedicated ledger.

Example:

Rahul

Total owed:
₹1,850

Transactions:

Dinner      ₹600
Movie       ₹350
Cab         ₹200
Lunch       ₹500
Coffee      ₹200

Total:
₹1,850

The user should be able to settle all outstanding debt at once.

---

# Two-Way Debt

The system must support both directions.

Example:

Rahul owes Ayush ₹1,850.

Ayush owes Rahul ₹300.

The system should calculate the net balance:

Rahul owes Ayush ₹1,550.

The application must maintain clear transaction history rather than creating confusing duplicated balances.

---

# Partial Settlement

A user may settle only part of a debt.

Example:

Outstanding:
₹1,850

Payment:
₹1,000

Remaining:
₹850

The remaining ₹850 must stay outstanding.

---

# Full Settlement

If the entire outstanding amount is paid:

Outstanding:
₹0

The historical transactions must remain visible.

Settlement should not destroy the financial history.

---

# Dashboard

The dashboard should immediately show:

- total amount the user is owed
- total amount the user owes
- net balance
- people who owe the user
- people the user owes

Example:

You are owed:
₹4,250

You owe:
₹1,150

Net:
₹3,100

People:

Rahul    +₹1,850
Rohit      +₹750
Aman       -₹200
Karan      +₹500
Priya      -₹950

Positive = they owe the user.

Negative = the user owes them.

---

# Add Expense

The basic expense flow must be extremely fast.

Required fields:

- amount
- person
- description

Example:

Amount: ₹600
Person: Rahul
Description: Dinner

Optional advanced functionality can include:

- group
- multiple participants
- custom split
- percentage split
- shares

The basic person-to-person expense must remain simple.

---

# Groups

Groups are secondary.

Examples:

- Goa Trip
- Roommates
- College Friends
- Office Team

A group contains multiple users.

The group creator is its sole owner. The owner adds or removes registered users; adding a user makes them a member immediately. Members can raise a dispute using a fixed issue list, initially "I was added by mistake" and "An expense has the wrong amount". Only the owner can see all disputes and mark one resolved. Group membership does not expose members' private personal ledgers.

The group creator is its sole owner. The owner adds or removes registered users; adding a user makes them a member immediately. Members can raise a dispute using a fixed issue list, initially "I was added by mistake" and "An expense has the wrong amount". Only the owner can see all disputes and mark one resolved. Group membership does not expose members' private personal ledgers.

Expenses can be associated with a group.

---

# Bill Splitting

Initially support equal splitting.

Example:

₹2,500 dinner
5 participants

Each participant owes:
₹500

The resulting debts must feed into the same underlying ledger/balance system.

Do not create a completely separate financial system for groups.

---

# Payments

Users may optionally provide a UPI ID.

The application can provide:

- UPI payment link
- QR code
- "Pay ₹X" action

The application is NOT a bank or payment processor.

Actual payment is performed through the user's UPI/banking application.

Payment initiation must not automatically be treated as confirmed payment.

Initially, settlement confirmation can be user-driven.

## Private People Contacts

Users may add a personal ledger contact with a name, a phone number, or both. A phone-only contact uses the phone number as its display name. Users can send a friend request to a registered account by email; the recipient must accept it before the accounts become friends. Each user can keep a private nickname for the other person. Friend links must not expose either person's private ledger. Phone sign-in is not supported: Google remains the only sign-in method. Friend lookup by phone number is not part of the current flow.

---

# MVP

The first working version should contain:

1. User authentication via Google
2. Add people
3. Add person-to-person expense
4. View balance with each person
5. View detailed transaction history
6. Partial settlement
7. Full settlement
8. Dashboard
9. Groups
10. Equal bill splitting
11. UPI profile
12. UPI payment link/QR

Do not add unnecessary features before the MVP is stable.

---

### Authentication

Users authenticate through Google using OpenID Connect (OIDC).

The application does not store user passwords. After successful authentication, the backend creates or retrieves the corresponding application `User` and associates that user's expenses, settlements, groups, and other data with their application user ID.

Authentication identifies the user; authorization determines which data and operations that user is allowed to access.


# Future Features

Potential later features:

- custom splits
- percentage splits
- recurring expenses
- payment reminders
- notifications
- debt simplification
- receipt scanning
- OCR
- analytics
- export to CSV/PDF
- offline support
- push notifications
- biometric app lock

These are NOT part of the initial implementation unless explicitly requested.
