package com.splitledger.friend;

import java.util.UUID;

public class FriendSettlementReportNotFoundException extends RuntimeException {
    public FriendSettlementReportNotFoundException(UUID id) { super("Payment report not found: " + id); }
}
