package com.splitledger.expense;

import java.math.BigDecimal;
import java.util.UUID;

public interface PersonExpenseBalanceProjection {
    UUID getPersonId();
    String getDisplayName();
    BigDecimal getNetBalance();
}
