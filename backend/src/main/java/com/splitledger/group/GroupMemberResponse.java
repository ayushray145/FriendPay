package com.splitledger.group;

import java.time.Instant;
import java.util.UUID;

public record GroupMemberResponse(UUID membershipId, UUID userId, String displayName,
                                  GroupMemberRole role, GroupMemberStatus status, Instant joinedAt) {

    static GroupMemberResponse from(GroupMember member) {
        return new GroupMemberResponse(member.getId(), member.getUser().getId(), member.getUser().getDisplayName(),
                member.getRole(), member.getStatus(), member.getJoinedAt());
    }
}
