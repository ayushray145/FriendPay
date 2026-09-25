package com.splitledger.expense;

import java.math.BigDecimal;
import java.util.UUID;

public record PersonBalanceResponse(UUID personId, BigDecimal netBalance) {
}
