package com.example.roombooking.constant;

import com.example.roombooking.entity.Role;

public final class SecurityConstants {

    private SecurityConstants() {}

    // ===== JWT / Header =====
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_TYPE = "Bearer";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_USER_ID = "uid";

    // ===== Thời hạn mặc định (đơn vị khai báo trong tên) =====
    public static final long DEFAULT_ACCESS_TOKEN_EXPIRATION_MS = 15 * 60 * 1000L;          // 15 phút
    public static final long DEFAULT_REFRESH_TOKEN_EXPIRATION_DAYS = 7;                      // 7 ngày
    public static final int DEFAULT_PASSWORD_RESET_EXPIRATION_MINUTES = 15;                  // 15 phút

    // ===== Độ dài token ngẫu nhiên (byte) =====
    public static final int REFRESH_TOKEN_BYTES = 48;
    public static final int PASSWORD_RESET_TOKEN_BYTES = 32;

    // ===== Vai trò =====
    public static final String ROLE_ADMIN = Role.ADMIN;
    public static final String ROLE_USER = Role.USER;

    /** Dùng trong @PreAuthorize, vì hasRole() tự thêm tiền tố ROLE_. */
    public static final String HAS_ADMIN = "hasRole('ADMIN')";
    public static final String HAS_USER_OR_ADMIN = "hasAnyRole('USER','ADMIN')";

    // ===== Đường dẫn công khai (không cần đăng nhập) =====
    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/captcha",
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/admin/login",
            "/api/auth/admin/register",
            "/api/auth/refresh-token",
            "/api/auth/forgot-password",
            "/api/auth/verify-reset-token",
            "/api/auth/reset-password",
            "/api/files/public/**"
    };

    public static final String[] SWAGGER_ENDPOINTS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    // ===== Mật khẩu =====
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,100}$";
}
