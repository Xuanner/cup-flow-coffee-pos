CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    last_activity_at TIMESTAMPTZ NOT NULL,
    idle_expires_at TIMESTAMPTZ NOT NULL,
    absolute_expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revocation_reason VARCHAR(32),
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_auth_sessions_token_hash_sha256 CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_auth_sessions_activity_order CHECK (last_activity_at >= created_at),
    CONSTRAINT ck_auth_sessions_idle_expiry CHECK (idle_expires_at > last_activity_at),
    CONSTRAINT ck_auth_sessions_absolute_expiry CHECK (absolute_expires_at > created_at),
    CONSTRAINT ck_auth_sessions_expiry_order CHECK (idle_expires_at <= absolute_expires_at),
    CONSTRAINT ck_auth_sessions_updated_order CHECK (updated_at >= created_at),
    CONSTRAINT ck_auth_sessions_revocation_order CHECK (revoked_at IS NULL OR revoked_at >= created_at),
    CONSTRAINT ck_auth_sessions_revocation_pair CHECK (
        (revoked_at IS NULL AND revocation_reason IS NULL)
        OR (revoked_at IS NOT NULL AND revocation_reason IS NOT NULL)
    ),
    CONSTRAINT ck_auth_sessions_revocation_reason CHECK (
        revocation_reason IS NULL
        OR revocation_reason IN (
            'LOGOUT',
            'IDLE_TIMEOUT',
            'ABSOLUTE_TIMEOUT',
            'ACCOUNT_DISABLED',
            'REPLACED'
        )
    ),
    CONSTRAINT ck_auth_sessions_version_non_negative CHECK (version >= 0)
);

CREATE INDEX ix_auth_sessions_account_active
    ON auth_sessions (account_id)
    WHERE revoked_at IS NULL;

CREATE INDEX ix_auth_sessions_idle_expiry_active
    ON auth_sessions (idle_expires_at)
    WHERE revoked_at IS NULL;

CREATE INDEX ix_auth_sessions_absolute_expiry_active
    ON auth_sessions (absolute_expires_at)
    WHERE revoked_at IS NULL;

CREATE INDEX ix_auth_sessions_revoked_cleanup
    ON auth_sessions (revoked_at)
    WHERE revoked_at IS NOT NULL;

COMMENT ON TABLE auth_sessions IS
    'Server-side authentication sessions. Expired or revoked rows are retained briefly and removed by application cleanup.';
COMMENT ON COLUMN auth_sessions.token_hash IS
    'Lowercase hexadecimal SHA-256 digest of the opaque session token. The raw browser credential must never be stored.';
COMMENT ON COLUMN auth_sessions.idle_expires_at IS
    'Idle deadline derived from the last accepted protected request; expiry is enforced before physical cleanup.';
COMMENT ON COLUMN auth_sessions.absolute_expires_at IS
    'Non-extendable absolute session deadline measured from login.';
COMMENT ON COLUMN auth_sessions.version IS
    'Optimistic concurrency version for activity refresh and revocation.';
