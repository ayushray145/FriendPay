package com.splitledger.expense;

import com.splitledger.ledger.DebtDirection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateExpenseRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "Amount must have at most 17 integer digits and 2 decimal places")
        BigDecimal amount,
        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,
        DebtDirection debtDirection,
        Instant occurredAt) {
}
