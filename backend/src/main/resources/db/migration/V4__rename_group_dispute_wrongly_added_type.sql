ALTER TABLE group_disputes DROP CONSTRAINT ck_group_disputes_type;
UPDATE group_disputes SET dispute_type = 'WRONGLY_ADDED' WHERE dispute_type = 'WRONGFULLY_ADDED';
ALTER TABLE group_disputes ADD CONSTRAINT ck_group_disputes_type
    CHECK (dispute_type IN ('WRONGLY_ADDED', 'INCORRECT_AMOUNT'));
