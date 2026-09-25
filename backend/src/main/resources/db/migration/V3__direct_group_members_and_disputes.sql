UPDATE group_members
SET membership_status = 'ACTIVE', joined_at = COALESCE(joined_at, invited_at)
WHERE membership_status = 'PENDING';

ALTER TABLE group_members DROP CONSTRAINT ck_group_members_status;
ALTER TABLE group_members ADD CONSTRAINT ck_group_members_status
    CHECK (membership_status IN ('ACTIVE', 'REMOVED'));
ALTER TABLE group_members RENAME COLUMN invited_by_user_id TO added_by_user_id;
ALTER TABLE group_members RENAME COLUMN invited_at TO added_at;

ALTER TABLE group_expenses ADD CONSTRAINT uk_group_expenses_group_id UNIQUE (group_id, id);

CREATE TABLE group_disputes (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES ledger_groups (id),
    raised_by_user_id UUID NOT NULL REFERENCES app_users (id),
    group_expense_id UUID,
    dispute_type VARCHAR(32) NOT NULL,
    dispute_status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    resolved_by_user_id UUID REFERENCES app_users (id),
    CONSTRAINT fk_group_disputes_expense FOREIGN KEY (group_id, group_expense_id)
        REFERENCES group_expenses (group_id, id),
    CONSTRAINT ck_group_disputes_type CHECK (dispute_type IN ('WRONGLY_ADDED', 'INCORRECT_AMOUNT')),
    CONSTRAINT ck_group_disputes_target CHECK (
        (dispute_type = 'WRONGLY_ADDED' AND group_expense_id IS NULL)
        OR (dispute_type = 'INCORRECT_AMOUNT' AND group_expense_id IS NOT NULL)
    ),
    CONSTRAINT ck_group_disputes_status CHECK (dispute_status IN ('OPEN', 'RESOLVED')),
    CONSTRAINT ck_group_disputes_resolution CHECK (
        (dispute_status = 'OPEN' AND resolved_at IS NULL AND resolved_by_user_id IS NULL)
        OR (dispute_status = 'RESOLVED' AND resolved_at IS NOT NULL AND resolved_by_user_id IS NOT NULL)
    )
);
CREATE INDEX idx_group_disputes_group_status ON group_disputes (group_id, dispute_status, created_at);
