CREATE TABLE IF NOT EXISTS asset (
    code VARCHAR(20) PRIMARY KEY,
    limited_supply BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS wallet (
    user_id VARCHAR(100) NOT NULL,
    asset VARCHAR(20) NOT NULL,
    balance BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, asset),
    CONSTRAINT fk_wallet_asset FOREIGN KEY (asset) REFERENCES asset(code)
);

CREATE INDEX IF NOT EXISTS idx_wallet_user_asset
    ON wallet(user_id, asset);


CREATE TABLE IF NOT EXISTS ledger_entry (
    id BIGSERIAL PRIMARY KEY,
    debit_user VARCHAR(100) NOT NULL,
    credit_user VARCHAR(100) NOT NULL,
    asset VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL CHECK (amount > 0),
    idempotency_key VARCHAR(255) NOT NULL,
    reference VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ledger_asset FOREIGN KEY (asset) REFERENCES asset(code)
);

CREATE INDEX IF NOT EXISTS idx_ledger_idempotency
    ON ledger_entry(idempotency_key);

CREATE INDEX IF NOT EXISTS idx_ledger_debit_user
    ON ledger_entry(debit_user);

CREATE INDEX IF NOT EXISTS idx_ledger_credit_user
    ON ledger_entry(credit_user);


CREATE TABLE IF NOT EXISTS idempotency_key (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_idempotency_key_operation
        UNIQUE (idempotency_key, operation)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_lookup
    ON idempotency_key(idempotency_key, operation);