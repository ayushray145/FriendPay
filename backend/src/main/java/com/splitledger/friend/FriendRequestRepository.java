package com.splitledger.friend;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {
    boolean existsByPairLowUserIdAndPairHighUserIdAndStatusIn(
            UUID pairLowUserId, UUID pairHighUserId, List<FriendRequestStatus> statuses);

    List<FriendRequest> findAllByRequesterIdOrRecipientIdOrderByCreatedAtDesc(
            UUID requesterId, UUID recipientId);

    List<FriendRequest> findAllByRequesterIdAndStatusOrRecipientIdAndStatusOrderByCreatedAtDesc(
            UUID requesterId, FriendRequestStatus requesterStatus,
            UUID recipientId, FriendRequestStatus recipientStatus);

    Optional<FriendRequest> findByIdAndRecipientIdAndStatus(UUID id, UUID recipientId, FriendRequestStatus status);

    Optional<FriendRequest> findByIdAndStatus(UUID id, FriendRequestStatus status);
}
