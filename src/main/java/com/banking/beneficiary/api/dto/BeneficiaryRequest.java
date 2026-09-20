package com.banking.beneficiary.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BeneficiaryRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,30}") String bankCode,
    @NotBlank @Pattern(regexp = "\\d{4,30}") String accountNumber,
    @NotBlank @Size(max = 255) String accountName,
    @Size(max = 100) String nickname) {}
