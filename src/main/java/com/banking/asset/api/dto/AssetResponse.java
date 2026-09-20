package com.banking.asset.api.dto;

import com.banking.asset.domain.AssetType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssetResponse(
    Long id,
    AssetType assetType,
    String assetName,
    BigDecimal estimatedValue,
    String currency,
    String description,
    LocalDateTime updatedAt) {}
