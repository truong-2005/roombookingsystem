package com.example.roombooking.service;

import java.util.Optional;

import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.RefreshToken;

public interface RefreshTokenService {

    /** rawToken là chuỗi trả cho client; entity chỉ chứa bản băm. */
    record IssuedToken(String rawToken, RefreshToken entity) {}

    IssuedToken issue(AppUser user, String ipAddress, String userAgent);

    /** Đổi refresh token cũ lấy token mới (xoay vòng). Token cũ bị thu hồi. */
    IssuedToken rotate(String rawToken, String ipAddress, String userAgent);

    Optional<RefreshToken> find(String rawToken);

    void revoke(String rawToken);

    int revokeAll(Long userId);

    int revokeAllExcept(Long userId, String rawToken);

    long countActiveSessions(Long userId);

    /** Dọn token hết hạn / đã thu hồi (chạy định kỳ). */
    void purgeExpired();
}