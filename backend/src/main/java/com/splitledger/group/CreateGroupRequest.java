package com.splitledger.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank(message = "Group name is required")
        @Size(max = 120, message = "Group name must be at most 120 characters")
        String name) {
}
