package com.splitledger.friend;

import java.math.BigDecimal;
import java.util.UUID;

public interface FriendSettlementBalanceProjection {
    UUID getPayerUserId();
    UUID getRecipientUserId();
    BigDecimal getAmount();
}
