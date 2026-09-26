ALTER TABLE payment_profiles
    ADD COLUMN shared_with_friends BOOLEAN NOT NULL DEFAULT FALSE;
