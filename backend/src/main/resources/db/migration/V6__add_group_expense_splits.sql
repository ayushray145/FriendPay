CREATE TABLE group_expense_splits (
    id UUID PRIMARY KEY,
    group_expense_id UUID NOT NULL REFERENCES group_expenses (id),
    user_id UUID NOT NULL REFERENCES app_users (id),
    share_amount NUMERIC(19, 2) NOT NULL,
    CONSTRAINT uk_group_expense_splits_expense_user UNIQUE (group_expense_id, user_id),
    CONSTRAINT ck_group_expense_splits_nonnegative CHECK (share_amount >= 0)
);
CREATE INDEX idx_group_expense_splits_user ON group_expense_splits (user_id);
