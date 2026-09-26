package com.splitledger.friend;

import java.util.UUID;

public class FriendExpenseNotFoundException extends RuntimeException {
    public FriendExpenseNotFoundException(UUID id) { super("Friend expense proposal not found: " + id); }
}
