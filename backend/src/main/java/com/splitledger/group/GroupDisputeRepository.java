package com.splitledger.group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupDisputeRepository extends JpaRepository<GroupDispute, UUID> {
    List<GroupDispute> findAllByLedgerGroupIdOrderByCreatedAtDescIdDesc(UUID groupId);
    List<GroupDispute> findAllByLedgerGroupIdAndRaisedByIdOrderByCreatedAtDescIdDesc(UUID groupId, UUID userId);
    Optional<GroupDispute> findByIdAndLedgerGroupId(UUID disputeId, UUID groupId);
}
