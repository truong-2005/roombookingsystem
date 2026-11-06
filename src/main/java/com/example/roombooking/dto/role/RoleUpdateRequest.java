package com.example.roombooking.dto.role;

import jakarta.validation.constraints.Size;

public record RoleUpdateRequest(
        @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
        String description
) {}