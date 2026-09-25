package com.splitledger.group;

public class GroupAccountNotFoundException extends RuntimeException {
    public GroupAccountNotFoundException(String email) { super("No Split Ledger account exists for " + email); }
}
