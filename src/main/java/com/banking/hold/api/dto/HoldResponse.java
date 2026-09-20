package com.banking.hold.api.dto;

import com.banking.hold.domain.HoldStatus;
import java.math.BigDecimal;

public record HoldResponse(
    Long id,
    Long accountId,
    BigDecimal amount,
    String currency,
    HoldStatus status,
    String reference) {}
