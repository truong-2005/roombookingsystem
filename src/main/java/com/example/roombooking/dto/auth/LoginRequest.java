package com.example.roombooking.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Vui lòng nhập tên đăng nhập hoặc email")
        @Size(max = 100, message = "Tên đăng nhập hoặc email tối đa 100 ký tự")
        String usernameOrEmail,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password,

        @NotBlank(message = "Vui lòng tải và nhập CAPTCHA")
        String captchaId,

        @NotBlank(message = "Vui lòng nhập đáp án CAPTCHA")
        String captchaAnswer
) {}
