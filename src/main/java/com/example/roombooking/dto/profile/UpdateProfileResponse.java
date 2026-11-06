package com.example.roombooking.dto.profile;

public record UpdateProfileResponse(ProfileResponse profile, boolean avatarChanged, String message) {}