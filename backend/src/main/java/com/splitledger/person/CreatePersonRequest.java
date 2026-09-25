package com.splitledger.person;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePersonRequest(
        @Size(max = 160, message = "Display name must be at most 160 characters")
        String displayName,
        @Pattern(regexp = "^(?:\\+[1-9][0-9]{7,14}|[6-9][0-9]{9})$",
                message = "Enter a mobile number with country code, or a 10-digit Indian mobile number")
        String phoneNumber) {

    @AssertTrue(message = "Add a name or a valid phone number")
    public boolean hasContactDetails() {
        return (displayName != null && !displayName.isBlank())
                || (phoneNumber != null && !phoneNumber.isBlank());
    }
}
