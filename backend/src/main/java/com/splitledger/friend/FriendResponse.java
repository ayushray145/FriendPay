package com.splitledger.friend;

import java.time.Instant;
import java.util.UUID;

public record FriendResponse(UUID requestId, UUID userId, String email, String displayName,
                             String nickname, Instant friendsSince, boolean canReceivePayments) { }
