package com.banking.user.api.dto;

public record LoginResponse(
    String token, String tokenType, Long userId, String fullName, String email) {}
