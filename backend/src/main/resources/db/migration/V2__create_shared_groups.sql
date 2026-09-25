CREATE TABLE ledger_groups (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES app_users (id),
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_ledger_groups_owner_id UNIQUE (owner_user_id, id)
);
CREATE INDEX idx_ledger_groups_owner_name ON ledger_groups (owner_user_id, name);

CREATE TABLE group_members (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES ledger_groups (id),
    user_id UUID NOT NULL REFERENCES app_users (id),
    invited_by_user_id UUID REFERENCES app_users (id),
    membership_role VARCHAR(16) NOT NULL,
    membership_status VARCHAR(16) NOT NULL,
    invited_at TIMESTAMPTZ NOT NULL,
    joined_at TIMESTAMPTZ,
    CONSTRAINT uk_group_members_group_user UNIQUE (group_id, user_id),
    CONSTRAINT ck_group_members_role CHECK (membership_role IN ('OWNER', 'MEMBER')),
    CONSTRAINT ck_group_members_status CHECK (membership_status IN ('PENDING', 'ACTIVE', 'REMOVED')),
    CONSTRAINT ck_group_members_joined CHECK (
        (membership_status = 'ACTIVE' AND joined_at IS NOT NULL)
        OR (membership_status <> 'ACTIVE' AND joined_at IS NULL)
    )
);
CREATE INDEX idx_group_members_user_status ON group_members (user_id, membership_status);

CREATE TABLE group_expenses (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES ledger_groups (id),
    paid_by_user_id UUID NOT NULL REFERENCES app_users (id),
    recorded_by_user_id UUID NOT NULL REFERENCES app_users (id),
    amount NUMERIC(19, 2) NOT NULL,
    description VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_group_expenses_positive_amount CHECK (amount > 0)
);
CREATE INDEX idx_group_expenses_group_time ON group_expenses (group_id, occurred_at);
