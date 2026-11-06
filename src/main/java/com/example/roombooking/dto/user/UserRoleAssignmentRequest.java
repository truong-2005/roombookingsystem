package com.example.roombooking.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UserRoleAssignmentRequest(
        @NotEmpty(message = "Phải chọn ít nhất một vai trò")
        Set<@NotBlank(message = "Tên vai trò không được để trống") String> roles
) {}