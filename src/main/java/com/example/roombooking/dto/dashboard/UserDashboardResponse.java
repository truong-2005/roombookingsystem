package com.example.roombooking.dto.dashboard;

import com.example.roombooking.dto.booking.BookingResponse;

import java.util.List;

public record UserDashboardResponse(
        long totalBookings,
        long pendingBookings,
        long approvedBookings,
        long unreadNotifications,
        List<BookingResponse> upcomingBookings
) {}