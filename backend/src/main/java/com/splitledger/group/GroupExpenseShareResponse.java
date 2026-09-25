package com.splitledger.group;

import java.math.BigDecimal;
import java.util.UUID;

public record GroupExpenseShareResponse(UUID userId, String displayName, BigDecimal shareAmount) {

    static GroupExpenseShareResponse from(GroupExpenseSplit split) {
        return new GroupExpenseShareResponse(split.getUser().getId(), split.getUser().getDisplayName(),
                split.getShareAmount());
    }
}
