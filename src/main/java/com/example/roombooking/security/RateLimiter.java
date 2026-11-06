package com.example.roombooking.security;

import com.example.roombooking.exception.TooManyRequestsException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter thuần in-memory dùng Caffeine cache.
 *
 * <p><b>Chiến lược:</b>
 * <ul>
 *   <li><b>Đăng nhập (login):</b> tối đa 5 lần/phút theo IP.
 *       Nếu sai thêm → bị chặn thêm 15 phút.</li>
 *   <li><b>Đăng ký (register):</b> tối đa 3 lần/phút theo IP.</li>
 * </ul>
 *
 * <p><b>Lưu ý triển khai production:</b> Khi deploy nhiều instance (cluster),
 * cần thay bằng Redis-based rate limiter (ví dụ Bucket4j + Redis) để đếm đúng
 * qua tất cả các node.
 */
@Slf4j
@Component
public class RateLimiter {

    // ===================== Cấu hình giới hạn =====================

    /** Số lần đăng nhập tối đa trong cửa sổ 1 phút theo IP. */
    private static final int LOGIN_MAX_ATTEMPTS = 5;

    /** Thời gian cửa sổ đếm đăng nhập (giây). */
    private static final long LOGIN_WINDOW_SECONDS = 60L;

    /** Thời gian khóa sau khi vượt giới hạn đăng nhập (giây). */
    private static final long LOGIN_BLOCK_SECONDS = 15 * 60L; // 15 phút

    /** Số lần đăng ký tối đa trong cửa sổ 1 phút theo IP. */
    private static final int REGISTER_MAX_ATTEMPTS = 3;

    /** Thời gian cửa sổ đếm đăng ký (giây). */
    private static final long REGISTER_WINDOW_SECONDS = 60L;

    /** Số CAPTCHA được cấp tối đa trong một phút trên mỗi IP. */
    private static final int CAPTCHA_MAX_CHALLENGES = 20;

    private static final long CAPTCHA_WINDOW_SECONDS = 60L;

    // ===================== Cache =====================

    /**
     * Đếm số lần đăng nhập trong cửa sổ 1 phút.
     * Key: IP address. Value: số lần thử. Entry tự hết hạn sau 60 giây.
     */
    private final Cache<String, AtomicInteger> loginAttemptCache = Caffeine.newBuilder()
            .expireAfterWrite(LOGIN_WINDOW_SECONDS, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build();

    /**
     * Danh sách IP đang bị khóa vì đăng nhập sai quá nhiều.
     * Key: IP address. Value: không quan trọng. Entry tự hết hạn sau 15 phút.
     */
    private final Cache<String, Boolean> loginBlockedCache = Caffeine.newBuilder()
            .expireAfterWrite(LOGIN_BLOCK_SECONDS, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build();

    /**
     * Đếm số lần đăng ký trong cửa sổ 1 phút.
     * Key: IP address. Value: số lần thử.
     */
    private final Cache<String, AtomicInteger> registerAttemptCache = Caffeine.newBuilder()
            .expireAfterWrite(REGISTER_WINDOW_SECONDS, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build();

    private final Cache<String, AtomicInteger> captchaChallengeCache = Caffeine.newBuilder()
            .expireAfterWrite(CAPTCHA_WINDOW_SECONDS, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build();

    // ===================== Public API =====================

    /**
     * Gọi TRƯỚC khi xử lý đăng nhập.
     * Ném {@link TooManyRequestsException} nếu IP bị chặn hoặc vượt giới hạn.
     *
     * @param ip địa chỉ IP của client
     */
    public void checkLoginLimit(String ip) {
        // 1. Kiểm tra xem IP đang bị khóa không
        if (loginBlockedCache.getIfPresent(ip) != null) {
            log.warn("IP bị chặn đăng nhập: {}", ip);
            throw new TooManyRequestsException(
                    "Quá nhiều lần đăng nhập thất bại. Vui lòng thử lại sau " +
                    (LOGIN_BLOCK_SECONDS / 60) + " phút",
                    LOGIN_BLOCK_SECONDS);
        }

        // 2. Tăng bộ đếm, lấy giá trị mới
        AtomicInteger counter = loginAttemptCache.get(ip, k -> new AtomicInteger(0));
        int attempts = counter.incrementAndGet();

        log.debug("Login attempt #{} from IP: {}", attempts, ip);

        if (attempts > LOGIN_MAX_ATTEMPTS) {
            // Vượt giới hạn → khóa IP
            loginBlockedCache.put(ip, Boolean.TRUE);
            loginAttemptCache.invalidate(ip);
            log.warn("Khóa IP đăng nhập {} phút vì vượt {} lần: {}", LOGIN_BLOCK_SECONDS / 60, LOGIN_MAX_ATTEMPTS, ip);
            throw new TooManyRequestsException(
                    "Quá nhiều lần đăng nhập. IP của bạn bị tạm khóa " +
                    (LOGIN_BLOCK_SECONDS / 60) + " phút",
                    LOGIN_BLOCK_SECONDS);
        }
    }

    /**
     * Gọi khi đăng nhập <b>thành công</b> để reset bộ đếm của IP đó.
     *
     * @param ip địa chỉ IP của client
     */
    public void resetLoginLimit(String ip) {
        loginAttemptCache.invalidate(ip);
        loginBlockedCache.invalidate(ip);
        log.debug("Reset login rate limit cho IP: {}", ip);
    }

    /**
     * Gọi TRƯỚC khi xử lý đăng ký.
     * Ném {@link TooManyRequestsException} nếu IP vượt giới hạn đăng ký.
     *
     * @param ip địa chỉ IP của client
     */
    public void checkRegisterLimit(String ip) {
        AtomicInteger counter = registerAttemptCache.get(ip, k -> new AtomicInteger(0));
        int attempts = counter.incrementAndGet();

        log.debug("Register attempt #{} from IP: {}", attempts, ip);

        if (attempts > REGISTER_MAX_ATTEMPTS) {
            log.warn("IP vượt giới hạn đăng ký: {}", ip);
            throw new TooManyRequestsException(
                    "Bạn đã đăng ký quá nhiều lần. Vui lòng thử lại sau " +
                    LOGIN_WINDOW_SECONDS + " giây",
                    REGISTER_WINDOW_SECONDS);
        }
    }

    public void checkCaptchaChallengeLimit(String ip) {
        AtomicInteger counter = captchaChallengeCache.get(ip, key -> new AtomicInteger(0));
        if (counter.incrementAndGet() > CAPTCHA_MAX_CHALLENGES) {
            throw new TooManyRequestsException("Bạn yêu cầu CAPTCHA quá nhiều lần. Vui lòng thử lại sau một phút",
                    CAPTCHA_WINDOW_SECONDS);
        }
    }

    // ===================== Monitoring =====================

    /** Kiểm tra xem IP có đang bị khóa đăng nhập không (dùng cho admin/debug). */
    public boolean isLoginBlocked(String ip) {
        return loginBlockedCache.getIfPresent(ip) != null;
    }

    /** Xem số lần thử đăng nhập hiện tại của một IP. */
    public int getLoginAttempts(String ip) {
        AtomicInteger counter = loginAttemptCache.getIfPresent(ip);
        return counter != null ? counter.get() : 0;
    }
}
