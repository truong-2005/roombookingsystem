package com.example.roombooking.dto.booking;

import com.example.roombooking.entity.Booking.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long roomId,
        String roomCode,
        String roomName,
        Long userId,
        String userFullName,
        String title,
        String purpose,
        Integer attendees,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BookingStatus status,
        String rejectReason,
        LocalDateTime createdAt
) {}