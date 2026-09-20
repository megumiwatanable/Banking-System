package com.banking.citad.api.dto;

import com.banking.citad.domain.CitadStatus;
import java.time.LocalDateTime;

public record CitadInquiryResponse(
    Long id,
    String referenceNumber,
    Long transactionId,
    CitadStatus status,
    String reason,
    String bankResponse,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String processingMode) {}
