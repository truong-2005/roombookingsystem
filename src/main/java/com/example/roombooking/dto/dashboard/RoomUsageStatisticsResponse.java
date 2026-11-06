package com.example.roombooking.dto.dashboard;

public record RoomUsageStatisticsResponse(
        Long roomId,
        String roomCode,
        String roomName,
        long totalBookings,
        long approvedBookings,
        double totalHoursBooked,
        double utilizationPercent
) {}