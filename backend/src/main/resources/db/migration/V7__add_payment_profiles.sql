CREATE TABLE payment_profiles (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL UNIQUE REFERENCES app_users (id),
    upi_id VARCHAR(320) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
