package com.banking.customer.api.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CustomerProfileRequest(
    @Pattern(regexp = "[0-9+]{8,20}") String phoneNumber,
    @Size(max = 500) String address,
    @Past LocalDate dateOfBirth) {}
