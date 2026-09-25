package com.splitledger.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SavePaymentProfileRequest(
        @NotBlank(message = "UPI ID is required")
        @Size(max = 320, message = "UPI ID must be at most 320 characters")
        @Pattern(regexp = "(?i)^[a-z0-9][a-z0-9._-]{1,255}@[a-z0-9][a-z0-9.-]{1,63}$",
                message = "Enter a valid UPI ID, such as name@bank")
        String upiId) {
}
