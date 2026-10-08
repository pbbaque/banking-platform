CREATE TABLE customers (
    id RAW(16) NOT NULL,
    identity_user_id RAW(16) NOT NULL,

    first_name VARCHAR2(100) NOT NULL,
    last_name VARCHAR2(150) NOT NULL,

    document_type VARCHAR2(20) NOT NULL,
    document_number VARCHAR2(50) NOT NULL,

    date_of_birth DATE NOT NULL,

    phone_number VARCHAR2(30),

    fiscal_address VARCHAR2(255),
    postal_code VARCHAR2(20),
    city VARCHAR2(100),
    country_code CHAR(2),

    status VARCHAR2(30) DEFAULT 'PENDING_VERIFICATION' NOT NULL,

    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    deleted_at TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT pk_customers
        PRIMARY KEY (id),

    CONSTRAINT uq_customers_identity_user
        UNIQUE (identity_user_id),

    CONSTRAINT uq_customers_document
        UNIQUE (document_type, document_number),

    CONSTRAINT ck_customers_document_type
        CHECK (
            document_type IN (
                'DNI',
                'NIE',
                'PASSPORT',
                'OTHER'
            )
        ),

    CONSTRAINT ck_customers_status
        CHECK (
            status IN (
                'PENDING_VERIFICATION',
                'ACTIVE',
                'BLOCKED',
                'INACTIVE'
            )
        )
);

CREATE INDEX idx_customers_status
    ON customers (status);

CREATE INDEX idx_customers_deleted_at
    ON customers (deleted_at);

CREATE INDEX idx_customers_last_name
    ON customers (last_name);