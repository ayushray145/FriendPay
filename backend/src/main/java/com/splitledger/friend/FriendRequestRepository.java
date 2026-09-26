package com.splitledger.friend;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            select request from FriendRequest request
            where request.status = :status
              and ((request.requester.id = :firstUserId and request.recipient.id = :secondUserId)
                or (request.requester.id = :secondUserId and request.recipient.id = :firstUserId))
            """)
    Optional<FriendRequest> findBetweenUsersWithStatus(@Param("firstUserId") UUID firstUserId,
                                                       @Param("secondUserId") UUID secondUserId,
                                                       @Param("status") FriendRequestStatus status);
}
