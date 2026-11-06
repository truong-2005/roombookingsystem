package com.example.roombooking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình JWT lấy từ application.properties với prefix {@code app.jwt}.
 *
 * <pre>
 * app.jwt.secret=<base64-encoded-secret-256-bit>
 * app.jwt.access-token-expiration-ms=900000       # 15 phút
 * app.jwt.refresh-token-expiration-days=7
 * </pre>
 */
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {

    /** Secret key dạng chuỗi (nên dài ≥ 32 ký tự, lưu trong biến môi trường). */
    private String secret = "";

    /** Issuer ghi vào access token. */
    private String issuer = "room-booking-system";

    /** Thời hạn access token (ms). Mặc định 15 phút. */
    private long accessTokenExpirationMs = 15 * 60 * 1000L;

    /** Thời hạn refresh token (ngày). Mặc định 7 ngày. */
    private long refreshTokenExpirationDays = 7;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public long getAccessTokenExpirationMs() { return accessTokenExpirationMs; }
    public void setAccessTokenExpirationMs(long ms) { this.accessTokenExpirationMs = ms; }

    public long getRefreshTokenExpirationDays() { return refreshTokenExpirationDays; }
    public void setRefreshTokenExpirationDays(long days) { this.refreshTokenExpirationDays = days; }
}
