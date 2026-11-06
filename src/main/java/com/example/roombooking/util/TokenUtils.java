package com.example.roombooking.util;

import com.example.roombooking.constant.SecurityConstants;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class TokenUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenUtils() {}

    /** Sinh chuỗi ngẫu nhiên an toàn, dạng Base64 URL-safe (không có dấu =, + hay /). */
    public static String generateToken(int numBytes) {
        byte[] bytes = new byte[numBytes];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String generateRefreshToken() {
        return generateToken(SecurityConstants.REFRESH_TOKEN_BYTES);
    }

    public static String generatePasswordResetToken() {
        return generateToken(SecurityConstants.PASSWORD_RESET_TOKEN_BYTES);
    }

    /** Băm SHA-256 ra chuỗi hex, dùng khi muốn lưu hash của token thay vì token gốc. */
    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Không hỗ trợ thuật toán SHA-256", e);
        }
    }

    /** Lấy JWT từ header "Authorization: Bearer xxx". Trả về null nếu không hợp lệ. */
    public static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader != null
                && authorizationHeader.startsWith(SecurityConstants.TOKEN_PREFIX)
                && authorizationHeader.length() > SecurityConstants.TOKEN_PREFIX.length()) {
            return authorizationHeader.substring(SecurityConstants.TOKEN_PREFIX.length()).trim();
        }
        return null;
    }

    /** "nguyenvana@gmail.com" -> "ng*******@gmail.com" */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        int visible = Math.min(2, local.length());
        return local.substring(0, visible) + "*".repeat(Math.max(local.length() - visible, 1)) + domain;
    }
}