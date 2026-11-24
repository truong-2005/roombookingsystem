package com.example.roombooking.security;

import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.RefreshToken;
import com.example.roombooking.exception.UnauthorizedException;
import com.example.roombooking.repository.RefreshTokenRepository;
import com.example.roombooking.service.RefreshTokenService.IssuedToken;
import com.example.roombooking.service.impl.RefreshTokenServiceImpl;
import com.example.roombooking.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private AppUser sampleUser;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "expirationDays", 7L);

        sampleUser = new AppUser();
        sampleUser.setId(1L);
        sampleUser.setUsername("testuser");
        sampleUser.setEnabled(true);
    }

    @Test
    @DisplayName("issue tạo refresh token mới và lưu bản băm vào repository")
    void issue_CreatesAndSavesRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IssuedToken issued = refreshTokenService.issue(sampleUser, "127.0.0.1", "Mozilla");

        assertNotNull(issued);
        assertNotNull(issued.rawToken());
        assertFalse(issued.rawToken().isBlank());
        assertNotNull(issued.entity());
        assertEquals(sampleUser, issued.entity().getUser());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("rotate ném UnauthorizedException khi rawToken không tồn tại")
    void rotate_WhenTokenNotFound_ThrowsUnauthorizedException() {
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> refreshTokenService.rotate("non-existent-token", "127.0.0.1", "Mozilla"));
        assertNotNull(ex);
    }

    @Test
    @DisplayName("rotate phát hiện token đã thu hồi bị dùng lại: thu hồi toàn bộ phiên của user và ném lỗi")
    void rotate_WhenTokenAlreadyRevoked_RevokesAllSessionsAndThrowsUnauthorizedException() {
        String raw = "stolen-raw-token";
        String hashed = TokenUtils.sha256Hex(raw);

        RefreshToken oldRevokedToken = RefreshToken.builder()
                .token(hashed)
                .user(sampleUser)
                .expiryDate(LocalDateTime.now().plusDays(5))
                .revoked(true) // Đã bị thu hồi
                .build();

        when(refreshTokenRepository.findByToken(hashed)).thenReturn(Optional.of(oldRevokedToken));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> refreshTokenService.rotate(raw, "127.0.0.1", "Mozilla"));

        assertTrue(ex.getMessage().contains("không còn hiệu lực"));
        // Xác minh gọi revokeAllByUserId(Long) để bảo vệ tài khoản khỏi kẻ tấn công
        verify(refreshTokenRepository, times(1)).revokeAllByUserId(1L);
    }

    @Test
    @DisplayName("rotate ném UnauthorizedException khi token đã hết hạn")
    void rotate_WhenTokenExpired_ThrowsUnauthorizedException() {
        String raw = "expired-raw-token";
        String hashed = TokenUtils.sha256Hex(raw);

        RefreshToken expiredToken = RefreshToken.builder()
                .token(hashed)
                .user(sampleUser)
                .expiryDate(LocalDateTime.now().minusDays(1)) // Đã hết hạn
                .build();

        when(refreshTokenRepository.findByToken(hashed)).thenReturn(Optional.of(expiredToken));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> refreshTokenService.rotate(raw, "127.0.0.1", "Mozilla"));
        assertNotNull(ex);
    }

    @Test
    @DisplayName("rotate thành công thu hồi token cũ và phát hành token mới (Token Rotation)")
    void rotate_WhenValid_RevokesOldTokenAndIssuesNewToken() {
        String raw = "valid-raw-token";
        String hashed = TokenUtils.sha256Hex(raw);

        RefreshToken oldToken = RefreshToken.builder()
                .token(hashed)
                .user(sampleUser)
                .expiryDate(LocalDateTime.now().plusDays(5))
                .build();

        when(refreshTokenRepository.findByToken(hashed)).thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IssuedToken newIssued = refreshTokenService.rotate(raw, "127.0.0.1", "Mozilla");

        assertNotNull(newIssued);
        assertNotEquals(raw, newIssued.rawToken()); // Token mới phải khác token cũ
        assertTrue(oldToken.isRevoked()); // Token cũ phải được đánh dấu đã thu hồi
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // 1 lần lưu oldToken, 1 lần lưu newToken
    }
}
