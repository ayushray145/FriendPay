package com.splitledger.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

public record DashboardPersonBalanceResponse(UUID personId, String displayName, BigDecimal netBalance) {
}
