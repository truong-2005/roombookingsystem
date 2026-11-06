package com.example.roombooking.security;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.roombooking.config.JwtConfig;
import com.example.roombooking.constant.SecurityConstants;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.RefreshToken;
import com.example.roombooking.exception.ExpiredTokenException;
import com.example.roombooking.exception.InvalidTokenException;
import com.example.roombooking.repository.RefreshTokenRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Tạo, xác thực, và thu hồi Refresh Token.
 */
@Slf4j
@Service
@EnableConfigurationProperties(JwtConfig.class)
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtConfig jwtConfig;

    @java.beans.ConstructorProperties({"refreshTokenRepository", "jwtConfig"})
    public RefreshTokenService(
        RefreshTokenRepository refreshTokenRepository,
        JwtConfig jwtConfig
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtConfig = jwtConfig;
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ===================== Tạo token =====================

  
    @Transactional
    public RefreshToken create(AppUser user, HttpServletRequest request) {
        LocalDateTime expiry = LocalDateTime.now()
                .plusDays(jwtConfig.getRefreshTokenExpirationDays());

        RefreshToken token = RefreshToken.builder()
                .token(generateTokenValue())
                .user(user)
                .expiryDate(expiry)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .ipAddress(request != null ? extractIp(request) : null)
                .build();

        return refreshTokenRepository.save(token);
    }

    // ===================== Xác thực token =====================

    /**
     * Tìm và xác thực refresh token.
     *
     * @param tokenValue chuỗi token từ client
     * @return entity nếu hợp lệ
     * @throws InvalidTokenException nếu không tồn tại hoặc đã bị thu hồi
     * @throws ExpiredTokenException nếu đã hết hạn
     */
    @Transactional(readOnly = true)
    public RefreshToken verify(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException("Refresh token không hợp lệ"));

        if (token.isRevoked()) {
            throw new InvalidTokenException("Refresh token đã bị thu hồi");
        }
        if (token.isExpired()) {
            throw new ExpiredTokenException("Refresh token đã hết hạn, vui lòng đăng nhập lại");
        }
        return token;
    }

    // ===================== Thu hồi =====================

    /** Thu hồi một token cụ thể (đăng xuất đơn). */
    @Transactional
    public void revoke(String tokenValue) {
        refreshTokenRepository.findByToken(tokenValue).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    /** Thu hồi mọi phiên của người dùng (đăng xuất tất cả / đổi mật khẩu). */
    @Transactional
    public int revokeAll(Long userId) {
        return refreshTokenRepository.revokeAllByUserId(userId);
    }

    /** Thu hồi mọi phiên, giữ lại phiên hiện tại (rotate token). */
    @Transactional
    public int revokeAllExcept(Long userId, String keepToken) {
        return refreshTokenRepository.revokeAllByUserIdExcept(userId, keepToken);
    }

    // ===================== Dọn dẹp =====================

    /** Xóa các token hết hạn hoặc đã bị thu hồi — nên gọi từ @Scheduled job. */
    @Transactional
    public int cleanup() {
        return refreshTokenRepository.deleteExpiredOrRevoked(LocalDateTime.now());
    }

    // ===================== Private =====================

    private String generateTokenValue() {
        byte[] bytes = new byte[SecurityConstants.REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
