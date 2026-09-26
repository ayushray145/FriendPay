CREATE TABLE friend_expense_proposals (
    id UUID PRIMARY KEY,
    friend_request_id UUID NOT NULL REFERENCES friend_requests (id),
    requester_user_id UUID NOT NULL REFERENCES app_users (id),
    recipient_user_id UUID NOT NULL REFERENCES app_users (id),
    amount NUMERIC(19, 2) NOT NULL,
    description VARCHAR(500) NOT NULL,
    debt_direction VARCHAR(32) NOT NULL,
    proposal_status VARCHAR(16) NOT NULL,
    dispute_reason VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    approved_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_friend_expense_proposal_users CHECK (requester_user_id <> recipient_user_id),
    CONSTRAINT ck_friend_expense_proposal_amount CHECK (amount > 0),
    CONSTRAINT ck_friend_expense_proposal_direction CHECK (debt_direction IN ('PERSON_OWES_USER', 'USER_OWES_PERSON')),
    CONSTRAINT ck_friend_expense_proposal_status CHECK (proposal_status IN ('PENDING', 'DISPUTED', 'APPROVED')),
    CONSTRAINT ck_friend_expense_proposal_dispute CHECK (
        (proposal_status = 'DISPUTED' AND dispute_reason IS NOT NULL)
        OR (proposal_status <> 'DISPUTED' AND dispute_reason IS NULL)
    )
);
CREATE INDEX idx_friend_expense_proposals_inbox ON friend_expense_proposals (recipient_user_id, proposal_status, created_at DESC);
CREATE INDEX idx_friend_expense_proposals_outbox ON friend_expense_proposals (requester_user_id, proposal_status, created_at DESC);

CREATE TABLE friend_settlements (
    id UUID PRIMARY KEY,
    friend_request_id UUID NOT NULL REFERENCES friend_requests (id),
    payer_user_id UUID NOT NULL REFERENCES app_users (id),
    recipient_user_id UUID NOT NULL REFERENCES app_users (id),
    amount NUMERIC(19, 2) NOT NULL,
    settled_at TIMESTAMPTZ NOT NULL,
    created_by_user_id UUID NOT NULL REFERENCES app_users (id),
    CONSTRAINT ck_friend_settlement_users CHECK (payer_user_id <> recipient_user_id),
    CONSTRAINT ck_friend_settlement_amount CHECK (amount > 0)
);
CREATE INDEX idx_friend_settlements_pair_time ON friend_settlements (payer_user_id, recipient_user_id, settled_at DESC);
CREATE INDEX idx_friend_settlements_recipient_time ON friend_settlements (recipient_user_id, payer_user_id, settled_at DESC);
