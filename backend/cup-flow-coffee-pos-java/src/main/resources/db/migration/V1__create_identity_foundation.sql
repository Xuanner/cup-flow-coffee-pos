CREATE TABLE roles (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_roles_code_not_blank CHECK (btrim(code) <> ''),
    CONSTRAINT ck_roles_name_not_blank CHECK (btrim(name) <> '')
);

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    CONSTRAINT ck_accounts_username_not_blank CHECK (btrim(username) <> ''),
    CONSTRAINT ck_accounts_password_hash_not_blank CHECK (btrim(password_hash) <> ''),
    CONSTRAINT ck_accounts_display_name_not_blank CHECK (btrim(display_name) <> ''),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_accounts_version_non_negative CHECK (version >= 0)
);

CREATE TABLE account_roles (
    account_id UUID NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE RESTRICT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by UUID,
    PRIMARY KEY (account_id, role_id)
);

INSERT INTO roles (id, code, name)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'CASHIER', '收银员'),
    ('00000000-0000-0000-0000-000000000002', 'ADMIN', '管理员');

COMMENT ON COLUMN accounts.password_hash IS 'One-way password hash only; plaintext passwords are forbidden.';
COMMENT ON COLUMN accounts.status IS 'ACTIVE or DISABLED.';
