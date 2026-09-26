package com.splitledger.person;

import java.time.Instant;
import java.util.UUID;

public record PersonResponse(UUID id, String displayName, String phoneNumber, UUID linkedUserId, Instant createdAt) {

    static PersonResponse from(Person person) {
        return new PersonResponse(person.getId(), person.getDisplayName(), person.getPhoneNumber(),
                person.getLinkedUser() == null ? null : person.getLinkedUser().getId(), person.getCreatedAt());
    }
}
