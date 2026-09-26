package com.splitledger.group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    Optional<GroupMember> findByLedgerGroupIdAndUserId(UUID groupId, UUID userId);

    List<GroupMember> findAllByLedgerGroupIdAndStatusOrderByUserDisplayNameAsc(
            UUID groupId, GroupMemberStatus status);

    boolean existsByLedgerGroupIdAndUserIdAndStatus(UUID groupId, UUID userId, GroupMemberStatus status);

    @Modifying
    @Query("delete from GroupMember member where member.ledgerGroup.id = :groupId")
    int deleteAllByLedgerGroupId(@Param("groupId") UUID groupId);
}
