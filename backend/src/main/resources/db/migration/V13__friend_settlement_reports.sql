CREATE TABLE friend_settlement_reports (
    id UUID PRIMARY KEY,
    friend_request_id UUID NOT NULL REFERENCES friend_requests (id),
    payer_user_id UUID NOT NULL REFERENCES app_users (id),
    recipient_user_id UUID NOT NULL REFERENCES app_users (id),
    amount NUMERIC(19, 2) NOT NULL,
    payment_reference VARCHAR(80),
    report_status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    reviewed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_friend_settlement_report_users CHECK (payer_user_id <> recipient_user_id),
    CONSTRAINT ck_friend_settlement_report_amount CHECK (amount > 0),
    CONSTRAINT ck_friend_settlement_report_status CHECK (report_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_friend_settlement_report_reviewed CHECK (
        (report_status = 'PENDING' AND reviewed_at IS NULL)
        OR (report_status <> 'PENDING' AND reviewed_at IS NOT NULL)
    )
);
CREATE UNIQUE INDEX uk_friend_settlement_report_pending_pair
    ON friend_settlement_reports (payer_user_id, recipient_user_id)
    WHERE report_status = 'PENDING';
CREATE INDEX idx_friend_settlement_reports_recipient_status
    ON friend_settlement_reports (recipient_user_id, report_status, created_at DESC);
CREATE INDEX idx_friend_settlement_reports_payer_status
    ON friend_settlement_reports (payer_user_id, report_status, created_at DESC);
