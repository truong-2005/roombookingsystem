package com.example.roombooking.dto.auth;

public record ForgotPasswordResponse(String maskedEmail, int expiresInMinutes, String message) {}