CREATE UNIQUE INDEX uk_group_members_single_owner
    ON group_members (group_id)
    WHERE membership_role = 'OWNER';
