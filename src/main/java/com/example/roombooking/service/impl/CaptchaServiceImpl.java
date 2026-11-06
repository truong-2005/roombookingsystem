package com.example.roombooking.service.impl;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

import com.example.roombooking.dto.captcha.CaptchaChallengeResponse;
import com.example.roombooking.exception.BadRequestException;
import com.example.roombooking.service.CaptchaService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final long CHALLENGE_TTL_SECONDS = 300;
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Cache<String, ChallengeAnswer> answers = Caffeine.newBuilder()
            .expireAfterWrite(CHALLENGE_TTL_SECONDS, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build();

    @Override
    public CaptchaChallengeResponse createChallenge() {
        int first = RANDOM.nextInt(10, 100);
        int second = RANDOM.nextInt(10, 100);
        String question;
        int answer;

        if (RANDOM.nextBoolean()) {
            question = first + " + " + second + " = ?";
            answer = first + second;
        } else {
            int larger = Math.max(first, second);
            int smaller = Math.min(first, second);
            question = larger + " - " + smaller + " = ?";
            answer = larger - smaller;
        }

        String challengeId = UUID.randomUUID().toString();
        answers.put(challengeId, new ChallengeAnswer(Integer.toString(answer), 0));
        return new CaptchaChallengeResponse(challengeId, question, CHALLENGE_TTL_SECONDS);
    }

    @Override
    public void verify(String challengeId, String answer) {
        if (challengeId == null || challengeId.isBlank() || answer == null || answer.isBlank()) {
            throw invalidCaptcha();
        }

        AtomicBoolean verified = new AtomicBoolean(false);
        answers.asMap().computeIfPresent(challengeId.trim(), (id, challenge) -> {
            if (challenge.expectedAnswer().equals(normalize(answer))) {
                verified.set(true);
                return null; // A challenge is consumed only after a correct answer.
            }

            int failedAttempts = challenge.failedAttempts() + 1;
            return failedAttempts >= MAX_FAILED_ATTEMPTS
                    ? null
                    : new ChallengeAnswer(challenge.expectedAnswer(), failedAttempts);
        });

        if (!verified.get()) {
            throw invalidCaptcha();
        }
    }

    private String normalize(String answer) {
        String trimmed = answer.trim();
        try {
            return Integer.toString(Integer.parseInt(trimmed));
        } catch (NumberFormatException ignored) {
            return trimmed;
        }
    }

    private record ChallengeAnswer(String expectedAnswer, int failedAttempts) {}

    private BadRequestException invalidCaptcha() {
        return new BadRequestException(
                "Đáp án CAPTCHA không đúng, mã đã hết hạn hoặc không còn hiệu lực. Vui lòng kiểm tra đáp án hoặc tải mã mới.");
    }
}
