package com.splitledger.expense;

import com.splitledger.ledger.DebtDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID personId,
        BigDecimal amount,
        DebtDirection debtDirection,
        String description,
        Instant occurredAt) {

    static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getPerson().getId(),
                expense.getAmount(),
                expense.getDebtDirection(),
                expense.getDescription(),
                expense.getOccurredAt());
    }
}
