package com.example.roombooking.dto.room;

import jakarta.validation.constraints.NotNull;

public record RoomStatusRequest(
        @NotNull(message = "Trạng thái hoạt động không được để trống")
        Boolean active
) {}