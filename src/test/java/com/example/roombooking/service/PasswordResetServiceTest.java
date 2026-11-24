package com.example.roombooking.service;

import com.example.roombooking.dto.auth.ForgotPasswordRequest;
import com.example.roombooking.dto.auth.ForgotPasswordResponse;
import com.example.roombooking.dto.auth.ResetPasswordRequest;
import com.example.roombooking.dto.auth.ResetPasswordResponse;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.PasswordResetToken;
import com.example.roombooking.exception.InvalidTokenException;
import com.example.roombooking.repository.AppUserRepository;
import com.example.roombooking.repository.PasswordResetTokenRepository;
import com.example.roombooking.service.impl.PasswordResetServiceImpl;
import com.example.roombooking.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private AppUser sampleUser;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(passwordResetService, "expirationMinutes", 15);

        sampleUser = new AppUser();
        sampleUser.setId(5L);
        sampleUser.setUsername("testuser");
        sampleUser.setEmail("test@example.com");
        sampleUser.setFullName("Nguyen Van Test");
        sampleUser.setEnabled(true);
    }

    @Test
    @DisplayName("forgotPassword gửi email đặt lại khi email tồn tại và tài khoản hợp lệ")
    void forgotPassword_WhenEmailExists_SendsEmail() {
        ForgotPasswordRequest req = new ForgotPasswordRequest("test@example.com");

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(tokenRepository.countByUserIdAndCreatedAtAfter(eq(5L), any())).thenReturn(0L);
        when(userRepository.getReferenceById(5L)).thenReturn(sampleUser);

        ForgotPasswordResponse response = passwordResetService.forgotPassword(req);

        assertNotNull(response);
        assertEquals(15, response.expiresInMinutes());
        assertTrue(response.maskedEmail().contains("**")); // Masked email: te**@example.com
        verify(tokenRepository, times(1)).invalidateAllByUserId(5L);
        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendPasswordResetEmail(eq("test@example.com"), eq("Nguyen Van Test"), anyString(), eq(15));
    }

    @Test
    @DisplayName("forgotPassword vẫn trả về thông báo tương tự nhưng không gửi email khi email không tồn tại")
    void forgotPassword_WhenEmailNotFound_DoesNotSendEmail() {
        ForgotPasswordRequest req = new ForgotPasswordRequest("notfound@example.com");

        when(userRepository.findByEmailIgnoreCase("notfound@example.com")).thenReturn(Optional.empty());

        ForgotPasswordResponse response = passwordResetService.forgotPassword(req);

        assertNotNull(response);
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any(), anyInt());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("resetPassword ném InvalidTokenException khi không tìm thấy token trong database")
    void resetPassword_WhenTokenNotFound_ThrowsInvalidTokenException() {
        ResetPasswordRequest req = new ResetPasswordRequest("invalid-raw-token", "NewPassword123@", "NewPassword123@");

        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        InvalidTokenException ex = assertThrows(InvalidTokenException.class, () -> passwordResetService.resetPassword(req));
        assertNotNull(ex);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("resetPassword cập nhật mật khẩu mới và thu hồi phiên đăng nhập thành công")
    void resetPassword_WhenValid_UpdatesPasswordAndRevokesSessions() {
        String rawToken = "valid-raw-token";
        String hashedToken = TokenUtils.sha256Hex(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(hashedToken)
                .user(sampleUser)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .build();

        ResetPasswordRequest req = new ResetPasswordRequest(rawToken, "NewPassword123@", "NewPassword123@");

        when(tokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("NewPassword123@")).thenReturn("newHashedPassword");

        ResetPasswordResponse response = passwordResetService.resetPassword(req);

        assertNotNull(response);
        assertTrue(response.success());
        assertEquals("newHashedPassword", sampleUser.getPassword());
        assertTrue(resetToken.isUsed());

        verify(userRepository, times(1)).saveAndFlush(sampleUser);
        verify(tokenRepository, times(1)).saveAndFlush(resetToken);
        verify(refreshTokenService, times(1)).revokeAll(5L);
        verify(emailService, times(1)).sendPasswordChangedEmail("test@example.com", "Nguyen Van Test");
    }
}
