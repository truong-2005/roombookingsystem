package com.example.roombooking.dto.notification;

import com.example.roombooking.dto.common.PageResponse;

public record NotificationListResponse(
        long unreadCount,
        PageResponse<NotificationResponse> notifications
) {}