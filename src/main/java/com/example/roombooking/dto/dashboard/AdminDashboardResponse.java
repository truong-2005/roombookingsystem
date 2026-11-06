package com.example.roombooking.dto.dashboard;

import com.example.roombooking.dto.booking.BookingResponse;

import java.util.List;

public record AdminDashboardResponse(
        long totalUsers,
        long totalRooms,
        long activeRooms,
        long pendingApprovals,
        long bookingsToday,
        BookingStatisticsResponse bookingStatistics,
        List<RoomUsageStatisticsResponse> topRooms,
        List<BookingResponse> recentPendingBookings
) {}