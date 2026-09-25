package com.splitledger.friend;

import java.time.Instant;
import java.util.UUID;

public record FriendRequestResponse(UUID id, String direction, String status, String email,
                                   String displayName, String nickname, Instant createdAt) { }
