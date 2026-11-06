package com.example.roombooking.dto.booking;

import java.time.LocalDateTime;
import java.util.List;

public record BookingConflictResponse(
        boolean hasConflict,
        String message,
        List<ConflictItem> conflicts
) {
    public record ConflictItem(
            Long bookingId,
            String title,
            String userFullName,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {}
}