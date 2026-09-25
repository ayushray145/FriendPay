package com.splitledger.settlement;

import com.splitledger.ledger.DebtDirection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateSettlementRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "Amount must have at most 17 integer digits and 2 decimal places")
        BigDecimal amount,
        @NotNull(message = "Payment direction is required")
        DebtDirection paymentDirection,
        Instant settledAt) {
}
