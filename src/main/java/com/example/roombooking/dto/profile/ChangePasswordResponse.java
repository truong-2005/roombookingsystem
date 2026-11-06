package com.example.roombooking.dto.profile;

import java.time.LocalDateTime;

public record ChangePasswordResponse(LocalDateTime changedAt, boolean otherSessionsRevoked, String message) {}