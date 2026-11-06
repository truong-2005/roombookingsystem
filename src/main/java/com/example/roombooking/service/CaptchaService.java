package com.example.roombooking.service;

import com.example.roombooking.dto.captcha.CaptchaChallengeResponse;

public interface CaptchaService {

    CaptchaChallengeResponse createChallenge();

    void verify(String challengeId, String answer);
}
