package com.example.roombooking.dto.user;

import com.example.roombooking.entity.AppUser.AvatarSource;

import java.time.LocalDateTime;
import java.util.Set;

public record UserDetailResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        String department,
        AvatarSource avatarSource,
        String avatarUrl,
        Set<String> roles,
        boolean enabled,
        long totalBookings,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}