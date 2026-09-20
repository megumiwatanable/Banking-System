package com.banking.casa.api.dto;

import com.banking.casa.domain.CasaPackage;
import com.banking.casa.domain.CasaStatus;
import java.time.LocalDateTime;

public record CasaEnrollmentResponse(
    Long id,
    String settlementAccountNumber,
    CasaPackage packageType,
    CasaStatus status,
    LocalDateTime enrolledAt) {}
