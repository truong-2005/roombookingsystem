package com.example.roombooking.dto.auth;

import java.time.LocalDateTime;

public record RegisterResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String avatarUrl,
        LocalDateTime createdAt
) {}