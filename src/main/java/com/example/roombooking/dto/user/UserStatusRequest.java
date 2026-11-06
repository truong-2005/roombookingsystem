package com.example.roombooking.dto.user;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
        @NotNull(message = "Trạng thái không được để trống")
        Boolean enabled
) {}