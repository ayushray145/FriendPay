package com.splitledger.group;

import java.math.BigDecimal;
import java.util.UUID;

public record GroupBalanceResponse(UUID userId, String displayName, BigDecimal netBalance) { }
