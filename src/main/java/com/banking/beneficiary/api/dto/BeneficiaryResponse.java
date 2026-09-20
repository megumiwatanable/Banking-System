package com.banking.beneficiary.api.dto;

import java.time.LocalDateTime;

public record BeneficiaryResponse(
    Long id,
    String bankCode,
    String accountNumber,
    String accountName,
    String nickname,
    LocalDateTime createdAt) {}
