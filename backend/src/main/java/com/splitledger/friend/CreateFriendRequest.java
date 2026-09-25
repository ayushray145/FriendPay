package com.splitledger.friend;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFriendRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @Size(max = 80, message = "Nickname must be at most 80 characters") String nickname) { }
