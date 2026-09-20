ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'STAFF', 'ADMIN')),
    ADD CONSTRAINT ck_users_status CHECK (status IN ('PENDING', 'ACTIVE', 'LOCKED', 'SUSPENDED', 'CLOSED'));

ALTER TABLE accounts
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    ADD COLUMN hold_amount DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD CONSTRAINT ck_accounts_hold_non_negative CHECK (hold_amount >= 0),
    ADD CONSTRAINT ck_accounts_hold_not_above_balance CHECK (hold_amount <= balance),
    ADD CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'FROZEN', 'BLOCKED', 'CLOSED'));

ALTER TABLE transactions
    MODIFY transaction_type ENUM('DEPOSIT', 'WITHDRAW', 'TRANSFER', 'INTERNAL_TRANSFER',
      'INTERBANK_TRANSFER', 'BILL_PAYMENT', 'CARD_PAYMENT', 'LOAN_DISBURSEMENT',
      'LOAN_REPAYMENT', 'FEE', 'REVERSAL') NOT NULL,
    MODIFY status ENUM('PENDING', 'INITIATED', 'VALIDATING', 'PROCESSING', 'SUCCESS',
      'FAILED', 'REVERSING', 'REVERSED') NOT NULL,
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    ADD COLUMN fee DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN description VARCHAR(255),
    ADD COLUMN original_transaction_id BIGINT,
    ADD COLUMN idempotency_key VARCHAR(100),
    ADD CONSTRAINT fk_transactions_original FOREIGN KEY (original_transaction_id) REFERENCES transactions(id),
    ADD CONSTRAINT uk_transactions_idempotency UNIQUE (idempotency_key),
    ADD CONSTRAINT ck_transactions_fee_non_negative CHECK (fee >= 0);

CREATE TABLE ledger_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance_before DECIMAL(19,2) NOT NULL,
    balance_after DECIMAL(19,2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_ledger_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT fk_ledger_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT ck_ledger_type CHECK (entry_type IN ('DEBIT', 'CREDIT', 'HOLD', 'RELEASE', 'FEE', 'REVERSAL')),
    CONSTRAINT ck_ledger_amount_positive CHECK (amount > 0)
) ENGINE=InnoDB;
CREATE INDEX idx_ledger_transaction ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_account_created ON ledger_entries(account_id, created_at);

CREATE TABLE idempotency_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    request_hash CHAR(64) NOT NULL,
    transaction_id BIGINT,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_idempotency_user_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_idempotency_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id)
) ENGINE=InnoDB;

CREATE TABLE transaction_limits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    per_transaction_limit DECIMAL(19,2) NOT NULL,
    daily_limit DECIMAL(19,2) NOT NULL,
    CONSTRAINT uk_transaction_limits_user_currency UNIQUE (user_id, currency),
    CONSTRAINT fk_transaction_limits_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE beneficiaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    bank_code VARCHAR(30) NOT NULL,
    account_number VARCHAR(64) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_beneficiaries_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_beneficiary_owner_account UNIQUE (user_id, bank_code, account_number)
) ENGINE=InnoDB;

CREATE TABLE balance_holds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reference VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_balance_holds_reference UNIQUE (reference),
    CONSTRAINT fk_balance_holds_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT ck_balance_holds_amount CHECK (amount > 0),
    CONSTRAINT ck_balance_holds_status CHECK (status IN ('HELD', 'CAPTURED', 'RELEASED'))
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(60) NOT NULL,
    resource_type VARCHAR(60) NOT NULL,
    resource_id VARCHAR(100),
    request_id VARCHAR(100),
    result VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;
