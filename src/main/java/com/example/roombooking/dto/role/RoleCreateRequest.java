package com.example.roombooking.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RoleCreateRequest(
        @NotBlank(message = "Tên vai trò không được để trống")
        @Size(max = 50, message = "Tên vai trò tối đa 50 ký tự")
        @Pattern(regexp = "^ROLE_[A-Z0-9_]+$",
                message = "Tên vai trò phải bắt đầu bằng ROLE_ và chỉ gồm chữ in hoa, số, gạch dưới (ví dụ ROLE_MANAGER)")
        String name,

        @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
        String description
) {}