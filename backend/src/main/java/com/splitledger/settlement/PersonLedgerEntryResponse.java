package com.splitledger.settlement;

import com.splitledger.ledger.DebtDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PersonLedgerEntryResponse(
        UUID id,
        String type,
        BigDecimal amount,
        DebtDirection direction,
        String description,
        Instant occurredAt) {
}
