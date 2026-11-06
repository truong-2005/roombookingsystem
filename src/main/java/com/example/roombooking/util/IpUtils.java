package com.example.roombooking.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Tiện ích lấy địa chỉ IP thực của client,
 * hỗ trợ cả trường hợp đứng sau reverse proxy (Nginx, Cloudflare, AWS ALB...).
 */
public final class IpUtils {

    private IpUtils() {}

    /**
     * Thứ tự ưu tiên header (từ đáng tin nhất đến ít đáng tin nhất):
     * <ol>
     *   <li>CF-Connecting-IP – Cloudflare</li>
     *   <li>X-Real-IP – Nginx</li>
     *   <li>X-Forwarded-For – standard (lấy IP đầu tiên trong chuỗi)</li>
     *   <li>remoteAddr – kết nối trực tiếp không qua proxy</li>
     * </ol>
     *
     * <p><b>Lưu ý bảo mật:</b> X-Forwarded-For có thể bị giả mạo nếu không đứng sau proxy tin cậy.
     * Trong môi trường production, chỉ bật tính năng này nếu server đứng sau proxy đáng tin cậy.
     */
    public static String getClientIp(HttpServletRequest request) {
        // Cloudflare
        String ip = request.getHeader("CF-Connecting-IP");
        if (isValid(ip)) return ip;

        // Nginx
        ip = request.getHeader("X-Real-IP");
        if (isValid(ip)) return ip;

        // Standard load balancer header – lấy IP đầu tiên
        ip = request.getHeader("X-Forwarded-For");
        if (isValid(ip)) {
            // Chuỗi có thể là "client, proxy1, proxy2" → lấy phần tử đầu
            return ip.split(",")[0].trim();
        }

        // Kết nối trực tiếp
        return request.getRemoteAddr();
    }

    private static boolean isValid(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }
}
