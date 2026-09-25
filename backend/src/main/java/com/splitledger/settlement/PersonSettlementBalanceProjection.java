package com.splitledger.settlement;

import java.math.BigDecimal;
import java.util.UUID;

public interface PersonSettlementBalanceProjection {
    UUID getPersonId();
    BigDecimal getBalanceAdjustment();
}
