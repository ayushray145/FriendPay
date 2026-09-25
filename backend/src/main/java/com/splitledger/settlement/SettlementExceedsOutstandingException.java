package com.splitledger.settlement;

import java.math.BigDecimal;
import java.util.UUID;

public class SettlementExceedsOutstandingException extends RuntimeException {
    public SettlementExceedsOutstandingException(UUID personId, BigDecimal outstanding) {
        super("Settlement exceeds the outstanding balance for person " + personId + "; outstanding amount is " + outstanding);
    }
}
