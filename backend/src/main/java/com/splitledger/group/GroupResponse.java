package com.splitledger.group;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GroupResponse(UUID id, String name, Instant createdAt, List<GroupMemberResponse> members) {
}
