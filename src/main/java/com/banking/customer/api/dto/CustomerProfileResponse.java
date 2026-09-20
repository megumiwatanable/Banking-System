package com.banking.customer.api.dto;

import com.banking.customer.domain.KycStatus;
import java.time.LocalDate;

public record CustomerProfileResponse(
    Long id,
    String customerNumber,
    String fullName,
    String email,
    String phoneNumber,
    String address,
    LocalDate dateOfBirth,
    KycStatus kycStatus) {}
