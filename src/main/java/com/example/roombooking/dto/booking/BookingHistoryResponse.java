package com.example.roombooking.dto.booking;

import com.example.roombooking.entity.Booking.BookingStatus;

import java.time.LocalDateTime;

/** Một mốc trong lịch sử của lịch đặt: tạo, duyệt, từ chối, hủy, dời lịch. */
public record BookingHistoryResponse(
        Long bookingId,
        String action,
        BookingStatus status,
        String actorName,
        String note,
        LocalDateTime occurredAt
) {}