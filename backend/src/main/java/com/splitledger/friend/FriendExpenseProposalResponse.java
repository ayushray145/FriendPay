package com.splitledger.friend;

import com.splitledger.ledger.DebtDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FriendExpenseProposalResponse(
        UUID id,
        String requestDirection,
        String status,
        UUID otherUserId,
        String otherName,
        String otherNickname,
        BigDecimal amount,
        String description,
        DebtDirection debtDirection,
        String disputeReason,
        Instant createdAt) {

    static FriendExpenseProposalResponse from(FriendExpenseProposal proposal, UUID viewerId) {
        boolean outgoing = proposal.getRequester().getId().equals(viewerId);
        var other = outgoing ? proposal.getRecipient() : proposal.getRequester();
        String nickname = outgoing
                ? proposal.getFriendRequest().getRequesterNickname()
                : proposal.getFriendRequest().getRecipientNickname();
        DebtDirection direction = proposal.getDebtDirection();
        if (!outgoing) {
            direction = direction == DebtDirection.PERSON_OWES_USER
                    ? DebtDirection.USER_OWES_PERSON : DebtDirection.PERSON_OWES_USER;
        }
        return new FriendExpenseProposalResponse(proposal.getId(), outgoing ? "OUTGOING" : "INCOMING",
                proposal.getStatus().name(), other.getId(), other.getDisplayName(),
                nickname == null || nickname.isBlank() ? other.getDisplayName() : nickname,
                proposal.getAmount(), proposal.getDescription(), direction,
                proposal.getDisputeReason(), proposal.getCreatedAt());
    }
}
