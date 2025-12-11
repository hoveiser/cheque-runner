package com.cheque.chequerunner.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ChequeIssueRequest {
    @NotNull(message = "Drawer ID must not be null")
    @Positive(message = "Drawer ID must be a positive number")
    private Long drawerId;

    @NotNull(message = "Cheque number must not be null")
    @Pattern(regexp = "^[A-Z]{2}-\\d{4}-\\d{4}$",
            message = "Cheque number format must be: Two-Letters-Four-Digits-Four-Digits (e.g., YT-2025-0001).")
    private String number;

    @NotNull(message = "Amount must not be null")
    @Positive(message = "Amount must be a positive value")
    private BigDecimal amount;
}