package com.splitledger.settlement;

import com.splitledger.ledger.DebtDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SettlementResponse(
        UUID id,
        UUID personId,
        BigDecimal amount,
        DebtDirection paymentDirection,
        Instant settledAt) {

    static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(settlement.getId(), settlement.getPerson().getId(),
                settlement.getAmount(), settlement.getPaymentDirection(), settlement.getSettledAt());
    }
}
