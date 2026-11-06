package com.example.roombooking.dto.auth;

import com.example.roombooking.entity.AppUser.AvatarSource;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

public record AuthUserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        AvatarSource avatarSource,
        String avatarUrl,
        @ArraySchema(schema = @Schema(type = "string", allowableValues = {"ROLE_USER", "ROLE_ADMIN"}))
        Set<String> roles,
        boolean enabled
) {}
