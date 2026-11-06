package com.example.roombooking.service;

import com.example.roombooking.dto.auth.AuthUserResponse;
import com.example.roombooking.dto.auth.LoginRequest;
import com.example.roombooking.dto.auth.LoginResponse;
import com.example.roombooking.dto.auth.LogoutRequest;
import com.example.roombooking.dto.auth.LogoutResponse;
import com.example.roombooking.dto.auth.RefreshTokenRequest;
import com.example.roombooking.dto.auth.RefreshTokenResponse;
import com.example.roombooking.dto.auth.RegisterRequest;

public interface AuthService {

    /** POST /api/login */
    LoginResponse login(LoginRequest req, String ipAddress, String userAgent);

    /** POST /api/auth/admin/login (bắt buộc có ROLE_ADMIN) */
    LoginResponse adminLogin(LoginRequest req, String ipAddress, String userAgent);

    /** POST /api/register */
    LoginResponse register(RegisterRequest req, String ipAddress, String userAgent);

    /** POST /api/auth/admin/register */
    LoginResponse adminRegister(RegisterRequest req, String ipAddress, String userAgent);

    /** POST /api/auth/refresh-token */
    RefreshTokenResponse refreshToken(RefreshTokenRequest req, String ipAddress, String userAgent);

    /** Đăng xuất user: thu hồi refresh token được gửi lên. */
    LogoutResponse logout(LogoutRequest req);

    /** Đăng xuất admin: như trên nhưng token phải thuộc về tài khoản ADMIN. */
    LogoutResponse adminLogout(LogoutRequest req);

    /** Đăng xuất khỏi tất cả thiết bị của người đang đăng nhập. */
    LogoutResponse logoutAllDevices();

    AuthUserResponse me();
}
