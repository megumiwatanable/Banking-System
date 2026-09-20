package com.banking.credit.api.dto;

import java.time.LocalDateTime;

public record CreditRatingResponse(
    int score, String grade, String explanation, LocalDateTime evaluatedAt, String disclaimer) {}
