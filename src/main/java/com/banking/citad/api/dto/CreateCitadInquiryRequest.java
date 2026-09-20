package com.banking.citad.api.dto;

import jakarta.validation.constraints.*;

public record CreateCitadInquiryRequest(
    @NotNull Long transactionId, @NotBlank @Size(max = 1000) String reason) {}
