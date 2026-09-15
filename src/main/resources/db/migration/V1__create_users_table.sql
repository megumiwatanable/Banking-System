CREATE TABLE users (
                       id        BIGINT AUTO_INCREMENT PRIMARY KEY,
                       full_name VARCHAR(255) NOT NULL,
                       email     VARCHAR(255) NOT NULL,
                       password  VARCHAR(255) NOT NULL,

                       CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB;
