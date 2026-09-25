package com.splitledger.group;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

public record GroupExpenseResponse(UUID id, UUID groupId, BigDecimal amount, String description,
                                   UUID paidByUserId, String paidByName, UUID recordedByUserId, Instant occurredAt,
                                   List<GroupExpenseShareResponse> shares) {

    static GroupExpenseResponse from(GroupExpense expense, List<GroupExpenseSplit> splits) {
        return new GroupExpenseResponse(expense.getId(), expense.getLedgerGroup().getId(), expense.getAmount(),
                expense.getDescription(), expense.getPaidBy().getId(), expense.getPaidBy().getDisplayName(),
                expense.getRecordedBy().getId(), expense.getOccurredAt(),
                splits.stream().map(GroupExpenseShareResponse::from).toList());
    }
}
