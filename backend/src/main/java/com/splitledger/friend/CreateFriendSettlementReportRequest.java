package com.splitledger.friend;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateFriendSettlementReportRequest(
        @NotNull UUID friendUserId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @Size(max = 80) @Pattern(regexp = "[A-Za-z0-9._/-]*") String paymentReference) { }
