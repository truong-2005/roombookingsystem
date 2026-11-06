package com.example.roombooking.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.example.roombooking.config.JwtConfig;
import com.example.roombooking.constant.SecurityConstants;
import com.example.roombooking.entity.AppUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * Tạo và xác thực JWT access token.
 */
@Slf4j
@Service
@EnableConfigurationProperties(JwtConfig.class)
public class JwtService {

    private final JwtConfig jwtConfig;
    private final SecretKey signingKey;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.signingKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    // ===================== Tạo token =====================

    /**
     * Tạo access token cho người dùng đã xác thực.
     *
     * @param userDetails thông tin người dùng từ Spring Security
     * @return JWT access token dạng compact string
     */
    public String generateAccessToken(CustomUserDetails userDetails) {
        long now = System.currentTimeMillis();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuer(jwtConfig.getIssuer())
                .claim(SecurityConstants.CLAIM_USER_ID, userDetails.getUserId())
                .claim(SecurityConstants.CLAIM_ROLES, roles)
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtConfig.getAccessTokenExpirationMs()))
                .signWith(signingKey)
                .compact();
    }

    /** Tạo access token trực tiếp từ entity người dùng (dùng khi đăng nhập / refresh). */
    public String generateAccessToken(AppUser user) {
        return generateAccessToken(new CustomUserDetails(user));
    }

    // ===================== Parse / Validate =====================

    /**
     * Kiểm tra và parse JWT token.
     *
     * @param token compact JWT string
     * @return Claims nếu hợp lệ, null nếu không hợp lệ
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("JWT hết hạn: {}", e.getMessage());
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT không hợp lệ: {}", e.getMessage());
            return null;
        }
    }

    /** Lấy username (subject) từ token, trả null nếu token không hợp lệ. */
    public String extractUsername(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /** Kiểm tra token có còn hợp lệ không. */
    public boolean isTokenValid(String token) {
        return parseToken(token) != null;
    }

    public long getAccessTokenExpirationMs() {
        return jwtConfig.getAccessTokenExpirationMs();
    }

    /** Thời hạn access token tính bằng giây (giá trị {@code expiresIn} trả về cho client). */
    public long getAccessTokenExpirationSeconds() {
        return jwtConfig.getAccessTokenExpirationMs() / 1000;
    }
}
