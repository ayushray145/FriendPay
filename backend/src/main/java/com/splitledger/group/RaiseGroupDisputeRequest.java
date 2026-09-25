package com.splitledger.group;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RaiseGroupDisputeRequest(
        @NotNull(message = "Issue type is required") GroupDisputeType issueType,
        UUID groupExpenseId) {
}
