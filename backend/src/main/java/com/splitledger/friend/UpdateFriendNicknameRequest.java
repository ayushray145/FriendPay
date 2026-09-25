package com.splitledger.friend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFriendNicknameRequest(
        @NotBlank @Size(max = 80, message = "Nickname must be between 1 and 80 characters") String nickname) { }
