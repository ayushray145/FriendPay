package com.splitledger.payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentProfileResponse(UUID userId, String displayName, String upiId, Instant updatedAt,
                                     boolean sharedWithFriends) {

    static PaymentProfileResponse from(PaymentProfile profile) {
        return new PaymentProfileResponse(profile.getOwner().getId(), profile.getOwner().getDisplayName(),
                profile.getUpiId(), profile.getUpdatedAt(), profile.isSharedWithFriends());
    }
}
