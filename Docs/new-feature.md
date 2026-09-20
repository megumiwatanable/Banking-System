# AGENTS.md — Banking System

## 1. Project Overview

Build a simplified Banking System using Java and Spring Boot.

The purpose of this project is to simulate common banking business flows and demonstrate backend architecture used in financial systems.

The system should support:

* Customer management
* Authentication
* CASA accounts
* Balance management
* Internal fund transfer
* Interbank transfer
* Beneficiary management
* Transaction history
* Transaction limits
* Fees
* Hold / release balance
* Reversal
* Bill payment
* Credit card
* Loan
* Notifications
* Audit logs
* Reconciliation

The implementation should prioritize:

* Correct transaction handling
* Data consistency
* Security
* Idempotency
* Auditability
* Clear separation between business domains
* Failure handling

---

# 2. High-Level Architecture

Target architecture:

```text
                         Client
                           |
                           v
                     API Gateway
                           |
            +--------------+--------------+
            |              |              |
            v              v              v
         Auth          Customer        Account
        Service         Service        Service
                                           |
                                           v
                                         MySQL

                           |
                           v
                    Transfer Service
                     /      |       \
                    /       |        \
                   v        v         v
              Account     Limit      Fee
              Service    Service    Service
                   |
                   v
                Ledger
                Service
                   |
                   v
                  Kafka
             /      |       \
            v       v        v
     Notification  Audit  Reconciliation
       Service    Service     Service
```

Do not tightly couple domains unnecessarily.

Business logic must belong to service/domain layers and must not be implemented directly inside controllers.

---

# 3. Main Domains

The system contains the following major domains:

```text
Customer
Authentication
Account
Balance
Beneficiary
Transfer
Payment
Ledger
Card
Loan
Notification
Audit
Reconciliation
```

---

# 4. Customer Management

A customer represents a bank user.

Basic customer information:

```text
customerId
fullName
dateOfBirth
phoneNumber
email
identityNumber
status
createdAt
updatedAt
```

Customer status:

```text
PENDING
ACTIVE
LOCKED
SUSPENDED
CLOSED
```

Only an `ACTIVE` customer can perform financial transactions.

Basic flow:

```text
Register Customer
       |
       v
Validate Information
       |
       v
Create Customer
       |
       v
Create Login Credentials
       |
       v
Activate Customer
```

For this learning project, KYC may be simulated instead of integrating with a real identity provider.

---

# 5. Authentication

Authentication should use:

```text
Spring Security
JWT
BCrypt
```

Flow:

```text
POST /api/auth/login
        |
        v
Validate username/password
        |
        v
Generate JWT
        |
        v
Return access token
```

Protected APIs require:

```http
Authorization: Bearer <JWT>
```

Roles:

```text
CUSTOMER
STAFF
ADMIN
```

Never trust `customerId` received from the client for authorization.

The authenticated identity must be used to determine whether the user owns the requested resource.

---

# 6. CASA Account

CASA means Current Account / Savings Account.

An account contains:

```text
accountId
accountNumber
customerId
accountType
currency
balance
availableBalance
holdAmount
status
createdAt
```

Account types:

```text
CURRENT
SAVINGS
```

Account status:

```text
ACTIVE
FROZEN
BLOCKED
CLOSED
```

Only `ACTIVE` accounts can normally participate in transfers.

---

# 7. Balance Model

Do not treat all balance values as the same thing.

Use:

```text
balance
availableBalance
holdAmount
```

Basic relationship:

```text
availableBalance = balance - holdAmount
```

Example:

```text
balance          = 10,000,000
holdAmount       = 2,000,000
availableBalance = 8,000,000
```

A transaction must check `availableBalance`, not only `balance`.

Money must use `BigDecimal`.

Never use:

```java
float
double
```

for monetary values.

Currency must always be explicit.

Example:

```text
amount   = 1000000
currency = VND
```

---

# 8. Beneficiary Management

Customers can save beneficiary accounts.

Beneficiary information:

```text
beneficiaryId
customerId
bankCode
accountNumber
accountName
nickname
createdAt
```

Operations:

```text
Create beneficiary
Update beneficiary
Delete beneficiary
List beneficiaries
```

A customer can only manage their own beneficiaries.

---

# 9. Internal Transfer

Internal transfer means both accounts belong to the same bank.

Example:

```text
Account A
   |
   | 1,000,000 VND
   v
Account B
```

Main API:

```http
POST /api/transfers/internal
```

Example request:

```json
{
  "fromAccount": "100000001",
  "toAccount": "100000002",
  "amount": 1000000,
  "currency": "VND",
  "description": "Transfer money"
}
```

Business flow:

```text
Receive Request
      |
      v
Authenticate Customer
      |
      v
Validate Source Account Ownership
      |
      v
Validate Source Account Status
      |
      v
Validate Destination Account
      |
      v
Validate Amount
      |
      v
Check Transaction Limit
      |
      v
Check Available Balance
      |
      v
Check Idempotency
      |
      v
Create Transaction
      |
      v
Debit Source
      |
      v
Credit Destination
      |
      v
Create Ledger Entries
      |
      v
Mark Transaction SUCCESS
      |
      v
Publish Event
      |
      +----------------+
      |                |
      v                v
Notification        Audit Log
```

---

# 10. Transfer Validation

A transfer must fail when:

```text
amount <= 0

source account does not exist

destination account does not exist

source account does not belong to customer

source account is not ACTIVE

destination account is not ACTIVE

source == destination

currency mismatch

available balance < amount + fee

transaction limit exceeded
```

Return meaningful business error codes.

Example:

```text
ACCOUNT_NOT_FOUND
ACCOUNT_BLOCKED
INVALID_AMOUNT
INSUFFICIENT_BALANCE
LIMIT_EXCEEDED
CURRENCY_MISMATCH
DUPLICATE_TRANSACTION
```

Do not expose internal stack traces to API clients.

---

# 11. Idempotency

Financial APIs must support idempotency.

Client sends:

```http
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

Server stores:

```text
idempotencyKey
customerId
requestHash
transactionId
status
response
createdAt
```

If the same request is sent again:

```text
same key + same request
        |
        v
return previous result
```

Do NOT execute the transfer again.

If the same key is reused with different request data, reject it.

This protects against:

```text
double click
mobile retry
network timeout
gateway retry
client retry
```

---

# 12. Transaction State

Transfer transaction states:

```text
INITIATED
VALIDATING
PROCESSING
SUCCESS
FAILED
REVERSING
REVERSED
```

Typical successful flow:

```text
INITIATED
    |
    v
VALIDATING
    |
    v
PROCESSING
    |
    v
SUCCESS
```

Failure:

```text
PROCESSING
    |
    v
FAILED
```

Reversal:

```text
SUCCESS / PROCESSING
        |
        v
    REVERSING
        |
        v
     REVERSED
```

State changes must be validated.

Do not allow arbitrary status updates.

---

# 13. Ledger

Do not rely only on updating an account balance.

Every financial movement should produce ledger entries.

Example:

Customer A transfers:

```text
1,000,000 VND
```

to Customer B.

Ledger:

```text
Account A
DEBIT
1,000,000

Account B
CREDIT
1,000,000
```

Conceptually:

```text
Debit total = Credit total
```

Ledger record:

```text
ledgerEntryId
transactionId
accountId
entryType
amount
currency
balanceBefore
balanceAfter
createdAt
```

Entry types:

```text
DEBIT
CREDIT
HOLD
RELEASE
FEE
REVERSAL
```

Ledger records should be immutable.

Never silently modify historical ledger records.

Corrections should create compensating entries.

---

# 14. Database Transaction

Internal transfer operations that update balances and ledger entries must be atomic.

Conceptually:

```java
@Transactional
transfer() {

    validate();

    debit();

    credit();

    createLedgerEntries();

    updateTransaction();
}
```

If any critical operation fails:

```text
ROLLBACK
```

Never allow:

```text
Account A debited
Account B not credited
```

without a controlled recovery mechanism.

---

# 15. Concurrency

The system must protect balances against concurrent updates.

Example:

```text
Balance = 10M

Request A withdraws 8M
Request B withdraws 8M
```

Both requests must not independently see 10M and succeed.

Use an appropriate strategy such as:

```text
Pessimistic Locking

or

Optimistic Locking
```

Balance consistency is more important than maximum throughput for this learning implementation.

---

# 16. Transaction Limit

Support configurable transaction limits.

Example:

```text
perTransactionLimit
dailyLimit
```

Example:

```text
Per transaction = 100,000,000 VND
Daily limit     = 500,000,000 VND
```

Before transfer:

```text
requested amount
       +
amount transferred today
       <=
daily limit
```

Limits should not be hardcoded inside controller code.

---

# 17. Fee

Some transactions may have fees.

Example:

```text
transferAmount = 1,000,000
fee            = 10,000
```

Required available balance:

```text
1,010,000
```

Fee should have its own ledger entry.

Example:

```text
DEBIT Account A     1,000,000
DEBIT Account A        10,000 FEE
CREDIT Account B    1,000,000
CREDIT Fee Account     10,000
```

Fee rules should be configurable.

---

# 18. Hold Balance

Some operations should reserve money before final settlement.

Example:

```text
Balance = 10M
```

Hold:

```text
2M
```

Result:

```text
balance          = 10M
holdAmount       = 2M
availableBalance = 8M
```

Possible flow:

```text
AVAILABLE
   |
   v
HOLD
   |
   +--------+
   |        |
   v        v
CAPTURE   RELEASE
```

`CAPTURE` converts reserved money into an actual debit.

`RELEASE` returns the reserved amount to available balance.

---

# 19. Interbank Transfer

Interbank transfer means sending money to another bank.

Flow:

```text
Customer
   |
   v
Transfer Service
   |
   v
Validate
   |
   v
Hold / Debit
   |
   v
Create Transfer
   |
   v
External Payment Adapter
   |
   v
External Banking Network
   |
   +---------------------+
   |                     |
 SUCCESS                FAILED
   |                     |
   v                     v
Finalize              Reversal
```

Do not directly integrate business logic with an external provider.

Use an abstraction:

```text
InterbankTransferProvider
```

Example implementations:

```text
MockInterbankProvider
NapasProvider
CitadProvider
```

For local development, use a mock provider.

---

# 20. Reversal

When a financial operation has been debited but cannot complete, the system may need reversal.

Example:

```text
Transfer
   |
   v
Debit 1M
   |
   v
External request
   |
   X
External failure
   |
   v
Create Reversal
   |
   v
Credit 1M back
```

Do not delete the original transaction.

Maintain:

```text
Original Transaction
+
Reversal Transaction
```

This provides an audit trail.

---

# 21. Bill Payment

Support simulated bill payments:

```text
Electricity
Water
Internet
Mobile
```

Flow:

```text
Customer
   |
   v
Select Provider
   |
   v
Enter Customer/Bill Code
   |
   v
Query Bill
   |
   v
Display Amount
   |
   v
Confirm
   |
   v
Debit Account
   |
   v
Pay Provider
   |
   v
SUCCESS
```

External bill providers should be mocked behind adapters.

---

# 22. Credit Card

Simplified credit card domain:

```text
Credit Card
    |
    +-- Credit Limit
    +-- Available Limit
    +-- Outstanding Balance
    +-- Statement
```

Important concepts:

```text
Authorization
Capture
Settlement
Refund
Reversal
```

Purchase flow:

```text
Purchase Request
       |
       v
Check Card Status
       |
       v
Check Available Limit
       |
       v
Authorization
       |
       v
Hold Credit Limit
       |
       v
Capture
       |
       v
Outstanding Balance
```

Do not mix CASA balance and credit-card available limit.

They represent different financial concepts.

---

# 23. Loan

Simplified loan flow:

```text
Customer
   |
   v
Loan Application
   |
   v
Validation
   |
   v
Credit Assessment
   |
   +---------+
   |         |
APPROVED   REJECTED
   |
   v
Disbursement
   |
   v
Repayment Schedule
   |
   v
Monthly Payment
   |
   v
CLOSED
```

Loan statuses:

```text
DRAFT
SUBMITTED
UNDER_REVIEW
APPROVED
REJECTED
DISBURSED
ACTIVE
OVERDUE
CLOSED
```

Loan concepts:

```text
principal
interestRate
term
monthlyPayment
outstandingPrincipal
interest
dueDate
```

For the learning project, credit scoring should be mocked.

---

# 24. Notification

Financial operations should publish events.

Example:

```text
TransferCompletedEvent
```

Consumer:

```text
Notification Service
```

Notification channels:

```text
EMAIL
SMS
PUSH
```

Example:

```text
Transfer Service

     |
     | Kafka
     v

transfer.completed

     |
     v

Notification Service
```

A notification failure must NOT roll back an already successful financial transaction.

---

# 25. Audit Log

Important actions must be auditable.

Record:

```text
userId
action
resourceType
resourceId
timestamp
ipAddress
requestId
result
```

Example actions:

```text
LOGIN
CREATE_ACCOUNT
TRANSFER
BILL_PAYMENT
CARD_PAYMENT
LOAN_APPLICATION
ACCOUNT_BLOCK
REVERSAL
```

Audit records should not be casually editable.

Never store:

```text
password
JWT
OTP
CVV
full card secrets
```

inside logs.

---

# 26. Transaction History

API:

```http
GET /api/transactions
```

Support:

```text
pagination
date range
transaction type
transaction status
account
```

Example transaction types:

```text
INTERNAL_TRANSFER
INTERBANK_TRANSFER
BILL_PAYMENT
CARD_PAYMENT
LOAN_DISBURSEMENT
LOAN_REPAYMENT
FEE
REVERSAL
```

A customer can only access transaction history belonging to accounts they own.

---

# 27. Reconciliation

Reconciliation compares internal records with external provider records.

Example:

```text
Internal System

TX001 SUCCESS 1M
TX002 SUCCESS 2M
TX003 SUCCESS 3M
```

External provider:

```text
TX001 SUCCESS 1M
TX002 SUCCESS 2M
TX003 MISSING
```

Result:

```text
TX003 -> MISMATCH
```

Reconciliation statuses:

```text
MATCHED
MISSING_INTERNAL
MISSING_EXTERNAL
AMOUNT_MISMATCH
STATUS_MISMATCH
```

Do not automatically change financial records solely because reconciliation found a mismatch.

Flag the issue for investigation or a controlled recovery workflow.

---

# 28. Error Handling

Use standardized API responses.

Example:

```json
{
  "code": "INSUFFICIENT_BALANCE",
  "message": "Available balance is insufficient",
  "requestId": "abc-123",
  "timestamp": "2026-09-21T10:30:00"
}
```

Common business errors:

```text
CUSTOMER_NOT_FOUND
CUSTOMER_BLOCKED

ACCOUNT_NOT_FOUND
ACCOUNT_BLOCKED

INVALID_AMOUNT
INSUFFICIENT_BALANCE

LIMIT_EXCEEDED

TRANSACTION_NOT_FOUND
DUPLICATE_TRANSACTION

BENEFICIARY_NOT_FOUND

EXTERNAL_PROVIDER_ERROR
```

HTTP status and business error code should be treated separately.

---

# 29. Database

Recommended local stack:

```text
MySQL
Redis
Kafka
```

Core tables:

```text
customers
users
accounts
beneficiaries

transactions
transaction_limits

ledger_entries

balance_holds

cards
card_transactions

loans
loan_schedules

notifications

audit_logs

idempotency_records

reconciliation_records
```

Use database migrations.

Recommended:

```text
Flyway
```

Do not rely on Hibernate automatically modifying production schemas.

---

# 30. Redis

Redis can be used for:

```text
rate limiting
temporary OTP
distributed locking
short-lived cache
idempotency optimization
```

Redis must not be treated as the authoritative source for financial balances.

The persistent financial record must remain in the transactional database / ledger.

---

# 31. Kafka

Kafka should be used for asynchronous events.

Possible topics:

```text
customer.created

account.created

transfer.completed
transfer.failed
transfer.reversed

bill-payment.completed

card.transaction.completed

loan.disbursed

notification.requested
```

Consumers must handle duplicate messages.

Do not assume exactly-once business execution simply because Kafka is used.

Use idempotent consumers.

---

# 32. Observability

Every request should have:

```text
requestId
correlationId
```

Example:

```text
API Gateway
     |
     | correlationId = ABC
     v
Transfer Service
     |
     | ABC
     v
Account Service
     |
     | ABC
     v
Kafka
     |
     | ABC
     v
Notification Service
```

Logs should make it possible to trace one transaction across services.

Provide:

```text
structured logging
metrics
health checks
```

Recommended:

```text
Spring Boot Actuator
Prometheus
Grafana
OpenSearch / ELK
```

---

# 33. Security Rules

Never:

```text
store plaintext passwords

log passwords

log JWT tokens

log OTP

trust customerId from frontend

allow customers to query other customers' accounts

use floating-point numbers for money

return internal stack traces

allow unrestricted account balance updates
```

Sensitive operations should require proper authorization.

---

# 34. API Structure

Example API groups:

```text
/api/auth/**

/api/customers/**

/api/accounts/**

/api/beneficiaries/**

/api/transfers/**

/api/payments/**

/api/cards/**

/api/loans/**

/api/transactions/**
```

Example:

```text
POST /api/auth/register
POST /api/auth/login

GET  /api/accounts
GET  /api/accounts/{accountId}

POST /api/beneficiaries
GET  /api/beneficiaries

POST /api/transfers/internal
POST /api/transfers/interbank

GET  /api/transactions

POST /api/payments/bills

GET  /api/cards
GET  /api/cards/{id}/transactions

POST /api/loans/applications
GET  /api/loans
```

---

# 35. Coding Rules

Use:

```text
Java 21+
Spring Boot
Spring Security
Spring Data JPA
MySQL
Redis
Kafka
Docker
Maven
Flyway
JUnit
Testcontainers
```

Recommended package structure:

```text
com.bank

├── auth
├── customer
├── account
├── beneficiary
├── transfer
├── payment
├── ledger
├── card
├── loan
├── notification
├── audit
├── reconciliation
├── common
└── config
```

Each domain may contain:

```text
controller
service
repository
entity
dto
mapper
exception
event
```

Controllers should:

```text
receive request
validate request format
call service
return response
```

Controllers must NOT contain complex business logic.

---

# 36. Implementation Strategy

Do NOT implement the entire project at once.

Implement incrementally.

## Phase 1 — Foundation

Implement:

```text
Spring Boot project
MySQL
Docker Compose
Flyway
Global exception handling
Base API response
```

## Phase 2 — Authentication

Implement:

```text
User
Customer
Register
Login
JWT
Spring Security
Roles
```

## Phase 3 — Account

Implement:

```text
CASA account
Account ownership
Balance
Available balance
Account status
```

## Phase 4 — Internal Transfer

This is the most important learning phase.

Implement:

```text
Transfer API
Validation
Transaction
Debit
Credit
Ledger
Idempotency
Concurrency protection
Transaction history
```

## Phase 5 — Event Driven

Add:

```text
Kafka
Transfer events
Notification
Audit
```

## Phase 6 — Interbank

Add:

```text
External provider abstraction
Mock external bank
Hold balance
Timeout
Retry
Reversal
```

## Phase 7 — Banking Features

Add:

```text
Bill Payment
Credit Card
Loan
```

## Phase 8 — Infrastructure

Add:

```text
Docker
Redis
Kafka
Prometheus
Grafana
OpenSearch
CI/CD
Kubernetes
```

---

# 37. Important Rule for Coding Agents

When generating code for this project:

1. Do not bypass business validation for convenience.
2. Do not directly modify balances from controllers.
3. Do not use `double` or `float` for money.
4. Financial database changes must be transactional.
5. Every money movement must be traceable.
6. Every transfer must support idempotency.
7. Protect balance operations from concurrency problems.
8. Never delete completed financial transactions.
9. Use reversal/compensating transactions instead of modifying history.
10. External integrations must be behind interfaces/adapters.
11. Async consumers must be idempotent.
12. Do not make notification delivery part of the financial database transaction.
13. Never expose another customer's banking information.
14. Add unit tests for business rules.
15. Add integration tests for critical money flows.
16. Explain important banking decisions in comments where useful.
17. Prefer simple implementations first, then introduce distributed-system complexity.
18. Do not create microservices solely for the sake of having microservices.

---

# 38. Critical Test Scenarios

At minimum, test:

```text
Transfer successfully

Insufficient balance

Invalid amount

Blocked account

Transfer to same account

Account ownership violation

Daily limit exceeded

Per-transaction limit exceeded

Duplicate Idempotency-Key

Same Idempotency-Key with different payload

Two concurrent transfers competing for the same balance

Debit succeeds but later operation throws exception

Reversal

External provider timeout

Duplicate Kafka event

Unauthorized account access
```

The most important concurrency test:

```text
Initial balance = 10M

Transfer A = 8M
Transfer B = 8M

Run concurrently.

Expected:

Only one transfer can succeed.

Final financial state must remain consistent.
```

---

# 39. Target Learning Outcome

The finished project should demonstrate understanding of:

```text
Java
Spring Boot
Spring Security
JWT

REST API

MySQL
JPA
Database Transaction
Database Locking

Banking Business Logic

Ledger
Debit / Credit
Balance / Available Balance
Hold / Release
Transfer
Reversal
Reconciliation

Redis
Kafka
Event Driven Architecture

Idempotency
Concurrency
Distributed Systems

Docker
Kubernetes
CI/CD

Logging
Monitoring
Security
System Design
```

The project should be treated as a learning banking platform rather than a production core-banking implementation.

Correctness and understanding of the business flow are more important than adding unnecessary features.
