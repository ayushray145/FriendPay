package com.splitledger.person;

import java.time.Instant;
import java.util.UUID;

public record PersonResponse(UUID id, String displayName, String phoneNumber, Instant createdAt) {

    static PersonResponse from(Person person) {
        return new PersonResponse(person.getId(), person.getDisplayName(), person.getPhoneNumber(), person.getCreatedAt());
    }
}
