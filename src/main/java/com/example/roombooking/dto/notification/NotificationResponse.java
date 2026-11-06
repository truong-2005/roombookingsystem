package com.example.roombooking.dto.notification;

import com.example.roombooking.entity.Notification.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long bookingId,
        NotificationType type,
        String title,
        String message,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {}