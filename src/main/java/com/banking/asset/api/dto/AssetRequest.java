package com.banking.asset.api.dto;

import com.banking.asset.domain.AssetType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AssetRequest(
    @NotNull AssetType assetType,
    @NotBlank @Size(max = 255) String assetName,
    @NotNull @PositiveOrZero BigDecimal estimatedValue,
    @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
    @Size(max = 1000) String description) {}
