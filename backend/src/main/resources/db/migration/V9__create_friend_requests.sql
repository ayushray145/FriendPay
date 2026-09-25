CREATE TABLE friend_requests (
    id UUID PRIMARY KEY,
    requester_user_id UUID NOT NULL REFERENCES app_users (id),
    recipient_user_id UUID NOT NULL REFERENCES app_users (id),
    pair_low_user_id UUID NOT NULL REFERENCES app_users (id),
    pair_high_user_id UUID NOT NULL REFERENCES app_users (id),
    requester_nickname VARCHAR(80),
    recipient_nickname VARCHAR(80),
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_friend_requests_distinct_users CHECK (requester_user_id <> recipient_user_id),
    CONSTRAINT ck_friend_requests_ordered_pair CHECK (pair_low_user_id < pair_high_user_id),
    CONSTRAINT ck_friend_requests_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED'))
);

CREATE UNIQUE INDEX uk_friend_requests_open_pair
    ON friend_requests (pair_low_user_id, pair_high_user_id)
    WHERE status IN ('PENDING', 'ACCEPTED');
CREATE INDEX idx_friend_requests_recipient_status ON friend_requests (recipient_user_id, status, created_at DESC);
CREATE INDEX idx_friend_requests_requester_status ON friend_requests (requester_user_id, status, created_at DESC);
