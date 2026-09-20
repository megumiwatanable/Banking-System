CREATE TABLE customer_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    customer_number VARCHAR(20) NOT NULL,
    phone_number VARCHAR(20),
    address VARCHAR(500),
    date_of_birth DATE,
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_customer_profiles_user UNIQUE (user_id),
    CONSTRAINT uk_customer_profiles_number UNIQUE (customer_number),
    CONSTRAINT fk_customer_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_customer_profiles_kyc CHECK (kyc_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
) ENGINE=InnoDB;

CREATE TABLE casa_enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    settlement_account_id BIGINT NOT NULL,
    package_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    enrolled_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_casa_enrollments_user UNIQUE (user_id),
    CONSTRAINT fk_casa_enrollments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_casa_enrollments_account FOREIGN KEY (settlement_account_id) REFERENCES accounts(id),
    CONSTRAINT ck_casa_package CHECK (package_type IN ('BASIC', 'PREMIUM')),
    CONSTRAINT ck_casa_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
) ENGINE=InnoDB;

CREATE TABLE customer_assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    asset_type VARCHAR(30) NOT NULL,
    asset_name VARCHAR(255) NOT NULL,
    estimated_value DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    description VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_customer_assets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_customer_assets_value CHECK (estimated_value >= 0)
) ENGINE=InnoDB;
CREATE INDEX idx_customer_assets_user ON customer_assets(user_id);

CREATE TABLE credit_ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    score INT NOT NULL,
    grade VARCHAR(5) NOT NULL,
    explanation VARCHAR(1000) NOT NULL,
    evaluated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_credit_ratings_user UNIQUE (user_id),
    CONSTRAINT fk_credit_ratings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_credit_rating_score CHECK (score BETWEEN 300 AND 850)
) ENGINE=InnoDB;

CREATE TABLE citad_inquiries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_number VARCHAR(30) NOT NULL,
    user_id BIGINT NOT NULL,
    transaction_id BIGINT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    bank_response VARCHAR(2000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_citad_reference UNIQUE (reference_number),
    CONSTRAINT fk_citad_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_citad_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT ck_citad_status CHECK (status IN ('RECEIVED', 'VERIFYING', 'SENT_TO_BANK', 'RESOLVED', 'REJECTED'))
) ENGINE=InnoDB;
CREATE INDEX idx_citad_user_created ON citad_inquiries(user_id, created_at);
