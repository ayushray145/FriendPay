package com.splitledger.friend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisputeFriendExpenseRequest(@NotBlank @Size(max = 500) String reason) { }
