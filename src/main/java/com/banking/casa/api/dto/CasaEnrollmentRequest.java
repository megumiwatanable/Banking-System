package com.banking.casa.api.dto;

import com.banking.casa.domain.CasaPackage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CasaEnrollmentRequest(
    @NotBlank String settlementAccountNumber, @NotNull CasaPackage packageType) {}
