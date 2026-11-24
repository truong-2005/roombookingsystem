package com.example.roombooking.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Vui lòng nhập tên đăng nhập hoặc email")
        @Size(max = 100, message = "Tên đăng nhập hoặc email tối đa 100 ký tự")
        String usernameOrEmail,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password,

        String captchaId,

        String captchaAnswer
) {
    public LoginRequest(String usernameOrEmail, String password) {
        this(usernameOrEmail, password, null, null);
    }
}
