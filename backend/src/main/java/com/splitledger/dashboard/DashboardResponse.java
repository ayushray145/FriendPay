package com.splitledger.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        BigDecimal totalOwedToYou,
        BigDecimal totalYouOwe,
        BigDecimal netBalance,
        List<DashboardPersonBalanceResponse> people) {
}
