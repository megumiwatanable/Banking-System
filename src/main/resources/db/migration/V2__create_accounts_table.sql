CREATE TABLE accounts (
                          id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                          account_number VARCHAR(64) NOT NULL,
                          account_type   ENUM('SAVINGS', 'CURRENT') NOT NULL,
                          balance        DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
                          user_id        BIGINT NOT NULL,
                          version        BIGINT NOT NULL DEFAULT 0,

                          CONSTRAINT uk_accounts_account_number UNIQUE (account_number),
                          CONSTRAINT fk_accounts_user FOREIGN KEY (user_id)
                              REFERENCES users (id) ON DELETE CASCADE,
                          CONSTRAINT ck_accounts_account_type
                              CHECK (account_type IN ('SAVINGS', 'CURRENT')),
                          CONSTRAINT ck_accounts_balance_non_negative
                              CHECK (balance >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_accounts_user_id ON accounts (user_id);
