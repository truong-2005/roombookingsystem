package com.example.roombooking.dto.booking;

import com.example.roombooking.dto.attachment.AttachmentResponse;
import com.example.roombooking.entity.Booking.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;

public record BookingDetailResponse(
        Long id,
        Long roomId,
        String roomCode,
        String roomName,
        Integer roomCapacity,
        Long userId,
        String userFullName,
        String userEmail,
        String userPhone,
        String userDepartment,
        String title,
        String purpose,
        Integer attendees,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BookingStatus status,
        String rejectReason,
        String reviewedByName,
        LocalDateTime reviewedAt,
        String cancelReason,
        LocalDateTime cancelledAt,
        List<AttachmentResponse> attachments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}