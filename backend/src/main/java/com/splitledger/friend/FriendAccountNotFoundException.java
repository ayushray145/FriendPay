package com.splitledger.friend;

public class FriendAccountNotFoundException extends RuntimeException {
    public FriendAccountNotFoundException() { super("No registered account was found for that email"); }
}
