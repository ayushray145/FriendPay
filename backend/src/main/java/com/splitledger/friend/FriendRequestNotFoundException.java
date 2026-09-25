package com.splitledger.friend;

import java.util.UUID;

public class FriendRequestNotFoundException extends RuntimeException {
    public FriendRequestNotFoundException(UUID id) { super("Friend request not found: " + id); }
}
