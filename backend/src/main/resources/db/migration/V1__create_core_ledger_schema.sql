CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    auth_provider VARCHAR(32) NOT NULL,
    auth_provider_subject VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_app_users_provider_subject UNIQUE (auth_provider, auth_provider_subject),
    CONSTRAINT uk_app_users_email UNIQUE (email)
);

CREATE TABLE people (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES app_users (id),
    linked_user_id UUID REFERENCES app_users (id),
    display_name VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_people_owner_id UNIQUE (owner_user_id, id),
    CONSTRAINT ck_people_not_self_link CHECK (linked_user_id IS NULL OR linked_user_id <> owner_user_id)
);
CREATE INDEX idx_people_owner ON people (owner_user_id);

CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES app_users (id),
    person_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    debt_direction VARCHAR(32) NOT NULL,
    description VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_expenses_owned_person FOREIGN KEY (owner_user_id, person_id)
        REFERENCES people (owner_user_id, id),
    CONSTRAINT ck_expenses_positive_amount CHECK (amount > 0),
    CONSTRAINT ck_expenses_direction CHECK (debt_direction IN ('PERSON_OWES_USER', 'USER_OWES_PERSON'))
);
CREATE INDEX idx_expenses_owner_person_time ON expenses (owner_user_id, person_id, occurred_at);

CREATE TABLE settlements (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES app_users (id),
    person_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    payment_direction VARCHAR(32) NOT NULL,
    settled_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_settlements_owned_person FOREIGN KEY (owner_user_id, person_id)
        REFERENCES people (owner_user_id, id),
    CONSTRAINT ck_settlements_positive_amount CHECK (amount > 0),
    CONSTRAINT ck_settlements_direction CHECK (payment_direction IN ('PERSON_OWES_USER', 'USER_OWES_PERSON'))
);
CREATE INDEX idx_settlements_owner_person_time ON settlements (owner_user_id, person_id, settled_at);
