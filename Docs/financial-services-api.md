# Financial services API

All endpoints require `Authorization: Bearer <token>`.

## Customer profile

- `GET /api/customer/me` creates or returns the caller's CIF profile.
- `PUT /api/customer/me` updates `phoneNumber`, `address`, and `dateOfBirth`.

KYC starts as `PENDING`. This MVP does not automatically verify identity documents.

## CASA

- `POST /api/casa/enroll` with `settlementAccountNumber` and `packageType` (`BASIC` or `PREMIUM`).
- `GET /api/casa/me` returns the current enrollment.

The settlement account must be an owned `CURRENT` account. Each customer can have one CASA enrollment.

## Transfers

- `POST /api/account/transfer` settles an internal transfer by account number.
- `POST /api/account/transfer/interbank` records a simulated interbank transfer as `PENDING`.

## Assets

- `GET /api/assets`
- `POST /api/assets`
- `PUT /api/assets/{id}`
- `DELETE /api/assets/{id}`

Supported types: `CASH`, `REAL_ESTATE`, `VEHICLE`, `STOCK`, `BOND`, `FUND`, `GOLD`, `OTHER`.

## Credit rating

- `POST /api/credit-rating/evaluate` recalculates the internal score.
- `GET /api/credit-rating/me` returns the last evaluation.

The score is an educational internal indicator based only on data stored in this application. It is not a CIC report or credit approval.

## CITAD inquiry

- `POST /api/citad/inquiries` with an owned interbank `transactionId` and `reason`.
- `GET /api/citad/inquiries`
- `GET /api/citad/inquiries/{id}`

New cases start at `RECEIVED`. The response explicitly reports `processingMode: SIMULATED`; no request is sent to the external CITAD system.
