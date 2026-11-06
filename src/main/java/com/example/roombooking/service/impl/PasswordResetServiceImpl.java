package com.example.roombooking.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.roombooking.dto.auth.ForgotPasswordRequest;
import com.example.roombooking.dto.auth.ForgotPasswordResponse;
import com.example.roombooking.dto.auth.ResetPasswordRequest;
import com.example.roombooking.dto.auth.ResetPasswordResponse;
import com.example.roombooking.dto.auth.VerifyResetTokenRequest;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.PasswordResetToken;
import com.example.roombooking.exception.AccountDisabledException;
import com.example.roombooking.exception.ExpiredTokenException;
import com.example.roombooking.exception.InvalidTokenException;
import com.example.roombooking.repository.AppUserRepository;
import com.example.roombooking.repository.PasswordResetTokenRepository;
import com.example.roombooking.service.EmailService;
import com.example.roombooking.service.PasswordResetService;
import com.example.roombooking.service.RefreshTokenService;
import com.example.roombooking.util.DateTimeUtils;
import com.example.roombooking.util.TokenUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int MAX_REQUESTS_PER_WINDOW = 3;
    private static final int WINDOW_MINUTES = 15;
    private static final String GENERIC_MESSAGE =
            "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư.";

    private final AppUserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @java.beans.ConstructorProperties({"userRepository", "tokenRepository", "refreshTokenService", "emailService", "passwordEncoder"})
    public PasswordResetServiceImpl(
        AppUserRepository userRepository,
        PasswordResetTokenRepository tokenRepository,
        RefreshTokenService refreshTokenService,
        EmailService emailService,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.password-reset.expiration-minutes:15}")
    private int expirationMinutes;

    @Override
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest req) {
        String email = req.email().trim().toLowerCase();
        // Luôn trả cùng một thông báo để người ngoài không dò được email nào có tài khoản
        ForgotPasswordResponse response =
                new ForgotPasswordResponse(TokenUtils.maskEmail(email), expirationMinutes, GENERIC_MESSAGE);

        Optional<AppUser> found = userRepository.findByEmailIgnoreCase(email);
        if (found.isEmpty() || !found.get().isEnabled()) {
            log.info("Yêu cầu quên mật khẩu cho email không tồn tại hoặc bị khóa");
            return response;
        }
        AppUser user = found.get();
        Long userId = user.getId();
        String toEmail = user.getEmail();
        String fullName = user.getFullName();

        LocalDateTime now = DateTimeUtils.now();
        if (tokenRepository.countByUserIdAndCreatedAtAfter(userId, now.minusMinutes(WINDOW_MINUTES))
                >= MAX_REQUESTS_PER_WINDOW) {
            log.warn("userId={} yêu cầu đặt lại mật khẩu quá nhiều lần, bỏ qua", userId);
            return response;
        }

        tokenRepository.invalidateAllByUserId(userId);
        String raw = TokenUtils.generatePasswordResetToken();
        tokenRepository.save(PasswordResetToken.builder()
                .token(TokenUtils.sha256Hex(raw))
                .user(userRepository.getReferenceById(userId))
                .expiryDate(now.plusMinutes(expirationMinutes))
                .build());

        String link = frontendUrl + "/reset-password?token=" + raw;
        emailService.sendPasswordResetEmail(toEmail, fullName, link, expirationMinutes);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ResetPasswordResponse verifyToken(VerifyResetTokenRequest req) {
        loadUsableToken(req.token());
        return new ResetPasswordResponse(true, "Mã đặt lại mật khẩu hợp lệ");
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest req) {
        PasswordResetToken token = loadUsableToken(req.token());
        AppUser user = token.getUser();
        Long userId = user.getId();
        String email = user.getEmail();
        String fullName = user.getFullName();

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        token.setUsed(true);
        userRepository.saveAndFlush(user);
        tokenRepository.saveAndFlush(token);

        tokenRepository.invalidateAllByUserId(userId);   // vô hiệu các mã khác còn lại
        refreshTokenService.revokeAll(userId);           // buộc đăng nhập lại ở mọi thiết bị
        emailService.sendPasswordChangedEmail(email, fullName);

        return new ResetPasswordResponse(true, "Đặt lại mật khẩu thành công, vui lòng đăng nhập bằng mật khẩu mới");
    }

    @Override
    @Scheduled(cron = "0 45 3 * * *")
    @Transactional
    public void purgeExpired() {
        int n = tokenRepository.deleteExpiredOrUsed(DateTimeUtils.now());
        log.info("Đã dọn {} mã đặt lại mật khẩu hết hạn hoặc đã dùng", n);
    }

    private PasswordResetToken loadUsableToken(String rawToken) {
        PasswordResetToken token = tokenRepository.findByToken(TokenUtils.sha256Hex(rawToken.trim()))
                .orElseThrow(InvalidTokenException::new);
        if (token.isUsed()) throw new InvalidTokenException();
        if (token.isExpired()) throw new ExpiredTokenException();
        if (!token.getUser().isEnabled()) throw new AccountDisabledException();
        return token;
    }
}