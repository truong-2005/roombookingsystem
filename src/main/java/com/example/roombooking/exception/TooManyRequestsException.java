package com.example.roombooking.exception;

/**
 * Ném ra khi người dùng vượt quá số lần thử cho phép.
 * GlobalExceptionHandler sẽ map sang HTTP 429 Too Many Requests.
 */
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public TooManyRequestsException(String message) {
        this(message, 60L);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
