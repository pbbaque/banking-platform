CREATE TABLE users (
    id RAW(16) PRIMARY KEY,
    email VARCHAR2(254) NOT NULL,
    password_hash VARCHAR2(255) NOT NULL,
    status VARCHAR2(30) DEFAULT 'PENDING' NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    last_login_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_status CHECK (
        status IN (
            'PENDING',
            'ACTIVE',
            'LOCKED',
            'DISABLED'
        )
    )
);
CREATE INDEX idx_users_status ON users(status);