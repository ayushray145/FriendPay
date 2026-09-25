package com.splitledger.security;

import java.util.UUID;

public class ApplicationUserNotFoundException extends RuntimeException {

    public ApplicationUserNotFoundException(UUID userId) {
        super("Authenticated application user not found: " + userId);
    }
}
