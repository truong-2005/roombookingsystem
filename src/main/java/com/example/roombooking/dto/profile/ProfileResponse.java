package com.example.roombooking.dto.profile;

import com.example.roombooking.entity.AppUser.AvatarSource;

import java.time.LocalDateTime;
import java.util.Set;

public record ProfileResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        String department,
        AvatarSource avatarSource,
        String avatarUrl,
        Set<String> roles,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {}