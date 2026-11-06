package com.example.roombooking.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record VerifyResetTokenRequest(
        @NotBlank(message = "Mã đặt lại mật khẩu không được để trống")
        String token
) {}