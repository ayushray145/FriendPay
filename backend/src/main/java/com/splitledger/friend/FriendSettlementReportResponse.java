package com.splitledger.friend;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FriendSettlementReportResponse(UUID id, String direction, String status, UUID otherUserId,
                                             String otherName, String otherNickname, BigDecimal amount,
                                             String paymentReference, Instant createdAt, Instant reviewedAt) {
    static FriendSettlementReportResponse from(FriendSettlementReport report, UUID viewerId) {
        boolean outgoing = report.getPayer().getId().equals(viewerId);
        var other = outgoing ? report.getRecipient() : report.getPayer();
        var request = report.getFriendRequest();
        String nickname = request.getRequester().getId().equals(viewerId)
                ? request.getRequesterNickname() : request.getRecipientNickname();
        return new FriendSettlementReportResponse(report.getId(), outgoing ? "OUTGOING" : "INCOMING",
                report.getStatus().name(), other.getId(), other.getDisplayName(),
                nickname == null || nickname.isBlank() ? other.getDisplayName() : nickname,
                report.getAmount(), report.getPaymentReference(), report.getCreatedAt(), report.getReviewedAt());
    }
}
