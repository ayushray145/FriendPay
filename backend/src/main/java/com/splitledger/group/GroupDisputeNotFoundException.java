package com.splitledger.group;

import java.util.UUID;

public class GroupDisputeNotFoundException extends RuntimeException {
    public GroupDisputeNotFoundException(UUID disputeId) { super("Group dispute not found: " + disputeId); }
}
