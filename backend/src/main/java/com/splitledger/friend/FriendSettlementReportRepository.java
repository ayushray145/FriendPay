package com.splitledger.friend;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendSettlementReportRepository extends JpaRepository<FriendSettlementReport, UUID> {
    List<FriendSettlementReport> findAllByPayerIdOrRecipientIdOrderByCreatedAtDesc(UUID payerId, UUID recipientId);
    boolean existsByPayerIdAndRecipientIdAndStatus(UUID payerId, UUID recipientId, FriendSettlementReportStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select report from FriendSettlementReport report where report.id = :id")
    Optional<FriendSettlementReport> findByIdForUpdate(@Param("id") UUID id);
}
