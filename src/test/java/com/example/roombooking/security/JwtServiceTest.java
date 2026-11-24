package com.example.roombooking.security;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.roombooking.config.JwtConfig;
import com.example.roombooking.constant.SecurityConstants;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.Role;

import io.jsonwebtoken.Claims;

class JwtServiceTest {

    private JwtConfig jwtConfig;
    private JwtService jwtService;
    private AppUser sampleUser;

    @BeforeEach
    public void setUp() {
        jwtConfig = new JwtConfig();
        // Secret key phải có độ dài tối thiểu 256 bits (32 bytes) cho thuật toán HMAC-SHA256
        jwtConfig.setSecret("test-secret-key-that-is-at-least-32-bytes-long-2026");
        jwtConfig.setIssuer("room-booking-system-test");
        jwtConfig.setAccessTokenExpirationMs(900_000L); // 15 phút

        jwtService = new JwtService(jwtConfig);

        Role userRole = new Role(1L, Role.USER, "Người dùng");
        sampleUser = new AppUser();
        sampleUser.setId(42L);
        sampleUser.setUsername("testuser");
        sampleUser.setEmail("test@example.com");
        sampleUser.setPassword("password123");
        sampleUser.setFullName("Nguyen Van A");
        sampleUser.setRoles(Set.of(userRole));
    }

    @Test
    @DisplayName("generateAccessToken sinh token dạng compact JWT không rỗng")
    void generateAccessToken_ReturnsNonEmptyJwtToken() {
        String token = jwtService.generateAccessToken(sampleUser);

        assertNotNull(token);
        assertFalse(token.isBlank());
        // JWT chuẩn gồm 3 phần ngăn cách bởi dấu chấm
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("parseToken phân tích chính xác các claims (subject, uid, issuer, roles)")
    void parseToken_ValidToken_ReturnsCorrectClaims() {
        String token = jwtService.generateAccessToken(sampleUser);

        Claims claims = jwtService.parseToken(token);
        assertNotNull(claims);
        assertEquals("testuser", claims.getSubject());
        assertEquals("room-booking-system-test", claims.getIssuer());
        assertEquals(42L, ((Number) claims.get(SecurityConstants.CLAIM_USER_ID)).longValue());

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get(SecurityConstants.CLAIM_ROLES);
        assertNotNull(roles);
        assertTrue(roles.contains(Role.USER));
    }

    @Test
    @DisplayName("extractUsername trích xuất đúng username từ token hợp lệ")
    void extractUsername_ValidToken_ReturnsUsername() {
        String token = jwtService.generateAccessToken(sampleUser);
        String username = jwtService.extractUsername(token);

        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("isTokenValid trả về true với token hợp lệ và còn hạn")
    void isTokenValid_ValidToken_ReturnsTrue() {
        String token = jwtService.generateAccessToken(sampleUser);
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("isTokenValid trả về false khi token là chuỗi rác hoặc sai chữ ký")
    void isTokenValid_InvalidToken_ReturnsFalse() {
        assertFalse(jwtService.isTokenValid("invalid.token.string"));
        assertFalse(jwtService.isTokenValid(""));
        assertFalse(jwtService.isTokenValid(null));
    }

    @Test
    @DisplayName("isTokenValid trả về false khi token đã hết hạn")
    void isTokenValid_ExpiredToken_ReturnsFalse() {
        // Cấu hình thời hạn âm để token hết hạn ngay lập tức
        JwtConfig expiredConfig = new JwtConfig();
        expiredConfig.setSecret("test-secret-key-that-is-at-least-32-bytes-long-2026");
        expiredConfig.setAccessTokenExpirationMs(-1000L);

        JwtService expiredJwtService = new JwtService(expiredConfig);
        String expiredToken = expiredJwtService.generateAccessToken(sampleUser);

        assertFalse(expiredJwtService.isTokenValid(expiredToken));
        assertNull(expiredJwtService.parseToken(expiredToken));
    }
}
