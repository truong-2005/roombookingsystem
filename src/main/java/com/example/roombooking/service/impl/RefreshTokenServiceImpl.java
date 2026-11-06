package com.example.roombooking.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.RefreshToken;
import com.example.roombooking.exception.AccountDisabledException;
import com.example.roombooking.exception.UnauthorizedException;
import com.example.roombooking.repository.RefreshTokenRepository;
import com.example.roombooking.service.RefreshTokenService;
import com.example.roombooking.util.DateTimeUtils;
import com.example.roombooking.util.TokenUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @java.beans.ConstructorProperties({"refreshTokenRepository"})
    public RefreshTokenServiceImpl(
        RefreshTokenRepository refreshTokenRepository
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Value("${app.refresh-token.expiration-days:7}")
    private long expirationDays;

    @Override
    @Transactional
    public IssuedToken issue(AppUser user, String ipAddress, String userAgent) {
        String raw = TokenUtils.generateRefreshToken();
        RefreshToken entity = RefreshToken.builder()
                .token(TokenUtils.sha256Hex(raw))
                .user(user)
                .expiryDate(DateTimeUtils.now().plusDays(expirationDays))
                .ipAddress(cut(ipAddress, 64))
                .userAgent(cut(userAgent, 255))
                .build();
        return new IssuedToken(raw, refreshTokenRepository.save(entity));
    }

    /** noRollbackFor: việc thu hồi toàn bộ phiên khi phát hiện dùng lại token phải được lưu dù ném lỗi. */
    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public IssuedToken rotate(String rawToken, String ipAddress, String userAgent) {
        RefreshToken old = refreshTokenRepository.findByToken(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Refresh token không hợp lệ"));

        if (old.isRevoked()) {
            // Token đã bị thu hồi mà vẫn được dùng lại: nghi ngờ bị đánh cắp, thu hồi mọi phiên của người này
            log.warn("Phát hiện dùng lại refresh token đã thu hồi, userId={}", old.getUser().getId());
            refreshTokenRepository.revokeAllByUserId(old.getUser().getId());
            throw new UnauthorizedException("Phiên đăng nhập không còn hiệu lực, vui lòng đăng nhập lại");
        }
        if (old.isExpired()) throw new UnauthorizedException("Refresh token đã hết hạn, vui lòng đăng nhập lại");
        if (!old.getUser().isEnabled()) throw new AccountDisabledException();

        old.setRevoked(true);
        refreshTokenRepository.save(old);
        return issue(old.getUser(), ipAddress, userAgent);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> find(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();
        return refreshTokenRepository.findByToken(hash(rawToken));
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        find(rawToken).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    @Override
    @Transactional
    public int revokeAll(Long userId) {
        return refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Override
    @Transactional
    public int revokeAllExcept(Long userId, String rawToken) {
        return refreshTokenRepository.revokeAllByUserIdExcept(userId, hash(rawToken));
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveSessions(Long userId) {
        return refreshTokenRepository.countByUserIdAndRevokedFalseAndExpiryDateAfter(userId, DateTimeUtils.now());
    }

    @Override
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpired() {
        int n = refreshTokenRepository.deleteExpiredOrRevoked(DateTimeUtils.now());
        log.info("Đã dọn {} refresh token hết hạn hoặc đã thu hồi", n);
    }

    private String hash(String raw) {
        return TokenUtils.sha256Hex(raw == null ? "" : raw.trim());
    }

    private String cut(String s, int max) {
        return s == null ? null : (s.length() <= max ? s : s.substring(0, max));
    }
}