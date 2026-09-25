package com.splitledger.group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    Optional<GroupMember> findByLedgerGroupIdAndUserId(UUID groupId, UUID userId);

    List<GroupMember> findAllByLedgerGroupIdAndStatusOrderByUserDisplayNameAsc(
            UUID groupId, GroupMemberStatus status);

    boolean existsByLedgerGroupIdAndUserIdAndStatus(UUID groupId, UUID userId, GroupMemberStatus status);
}
