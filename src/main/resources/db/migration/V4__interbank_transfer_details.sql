ALTER TABLE transactions
    MODIFY transaction_type ENUM('DEPOSIT', 'WITHDRAW', 'TRANSFER', 'INTERBANK_TRANSFER') NOT NULL,
    ADD COLUMN external_bank_code VARCHAR(30),
    ADD COLUMN external_account_number VARCHAR(64),
    ADD COLUMN external_recipient_name VARCHAR(255);

ALTER TABLE transactions DROP CONSTRAINT ck_transactions_transaction_type;
ALTER TABLE transactions ADD CONSTRAINT ck_transactions_transaction_type
    CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAW', 'TRANSFER', 'INTERBANK_TRANSFER'));
