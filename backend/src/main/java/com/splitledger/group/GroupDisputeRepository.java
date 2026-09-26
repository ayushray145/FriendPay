package com.splitledger.group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupDisputeRepository extends JpaRepository<GroupDispute, UUID> {
    List<GroupDispute> findAllByLedgerGroupIdOrderByCreatedAtDescIdDesc(UUID groupId);
    List<GroupDispute> findAllByLedgerGroupIdAndRaisedByIdOrderByCreatedAtDescIdDesc(UUID groupId, UUID userId);
    Optional<GroupDispute> findByIdAndLedgerGroupId(UUID disputeId, UUID groupId);

    @Modifying
    @Query("delete from GroupDispute dispute where dispute.ledgerGroup.id = :groupId")
    int deleteAllByLedgerGroupId(@Param("groupId") UUID groupId);
}
