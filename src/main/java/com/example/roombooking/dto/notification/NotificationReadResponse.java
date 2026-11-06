package com.example.roombooking.dto.notification;

public record NotificationReadResponse(int updatedCount, long unreadCount, String message) {}