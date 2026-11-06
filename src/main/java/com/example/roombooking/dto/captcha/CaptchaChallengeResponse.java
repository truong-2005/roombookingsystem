package com.example.roombooking.dto.captcha;

public record CaptchaChallengeResponse(
        String captchaId,
        String question,
        long expiresInSeconds
) {
}
