-- V7 may already be installed, so follow-up changes must be introduced in a new migration.
-- The old checks only allowed the transaction values that existed before V7.
ALTER TABLE transactions DROP CONSTRAINT ck_transactions_transaction_type;
ALTER TABLE transactions DROP CONSTRAINT ck_transactions_status;

ALTER TABLE transactions ADD CONSTRAINT ck_transactions_transaction_type
    CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAW', 'TRANSFER', 'INTERNAL_TRANSFER',
      'INTERBANK_TRANSFER', 'BILL_PAYMENT', 'CARD_PAYMENT', 'LOAN_DISBURSEMENT',
      'LOAN_REPAYMENT', 'FEE', 'REVERSAL'));

ALTER TABLE transactions ADD CONSTRAINT ck_transactions_status
    CHECK (status IN ('PENDING', 'INITIATED', 'VALIDATING', 'PROCESSING', 'SUCCESS',
      'FAILED', 'REVERSING', 'REVERSED'));

-- Every hold needs the financial transaction used by its immutable ledger entries.
ALTER TABLE balance_holds
    ADD COLUMN transaction_id BIGINT NULL AFTER account_id,
    ADD CONSTRAINT fk_balance_holds_transaction
      FOREIGN KEY (transaction_id) REFERENCES transactions(id);

-- Existing databases can only contain rows created outside the new application flow.
-- Leave those rows nullable; all newly-created holds always set transaction_id in Java.
