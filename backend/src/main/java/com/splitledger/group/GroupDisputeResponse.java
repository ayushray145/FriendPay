package com.splitledger.group;

import java.time.Instant;
import java.util.UUID;

public record GroupDisputeResponse(UUID id, UUID groupId, GroupDisputeType issueType, GroupDisputeStatus status,
                                   UUID raisedByUserId, String raisedByName, UUID groupExpenseId,
                                   String groupExpenseDescription, Instant createdAt, Instant resolvedAt,
                                   String resolvedByName) {

    static GroupDisputeResponse from(GroupDispute dispute) {
        GroupExpense expense = dispute.getGroupExpense();
        return new GroupDisputeResponse(dispute.getId(), dispute.getLedgerGroup().getId(), dispute.getIssueType(),
                dispute.getStatus(), dispute.getRaisedBy().getId(), dispute.getRaisedBy().getDisplayName(),
                expense == null ? null : expense.getId(), expense == null ? null : expense.getDescription(),
                dispute.getCreatedAt(), dispute.getResolvedAt(),
                dispute.getResolvedBy() == null ? null : dispute.getResolvedBy().getDisplayName());
    }
}
