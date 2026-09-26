package com.splitledger.friend;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendExpenseProposalRepository extends JpaRepository<FriendExpenseProposal, UUID> {

    List<FriendExpenseProposal> findAllByRequesterIdOrRecipientIdOrderByCreatedAtDesc(
            UUID requesterId, UUID recipientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select proposal from FriendExpenseProposal proposal where proposal.id = :id")
    Optional<FriendExpenseProposal> findByIdForUpdate(@Param("id") UUID id);

    List<FriendExpenseProposal> findAllByRequesterIdAndRecipientIdAndStatus(
            UUID requesterId, UUID recipientId, FriendExpenseProposalStatus status);
}
