package com.cheque.chequerunner.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class ChequeIssueRequest {
    @NotNull(message = "Drawer ID must not be null")
    @Positive(message = "Drawer ID must be a positive number")
    @Schema(description = "ID of the drawer account", example = "1")
    private Long drawerId;

    @NotNull(message = "Cheque number must not be null")
    @Pattern(regexp = "^[A-Z]{2}-\\d{4}-\\d{4}$",
            message = "Cheque number format must be: Two-Letters-Four-Digits-Four-Digits (e.g., YT-2025-0001).")
    @Schema(description = "Unique cheque number defined by the bank/system", example = "YT-2025-0001")
    private String number;

    @NotNull(message = "Amount must not be null")
    @Positive(message = "Amount must be a positive value")
    private BigDecimal amount;
}