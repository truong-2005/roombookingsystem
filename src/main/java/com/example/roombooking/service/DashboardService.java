package com.example.roombooking.service;

import java.time.LocalDate;
import java.util.List;

import com.example.roombooking.dto.dashboard.AdminDashboardResponse;
import com.example.roombooking.dto.dashboard.BookingStatisticsResponse;
import com.example.roombooking.dto.dashboard.RoomUsageStatisticsResponse;
import com.example.roombooking.dto.dashboard.UserDashboardResponse;

public interface DashboardService {

    AdminDashboardResponse getAdminDashboard();

    UserDashboardResponse getUserDashboard();

    /** Mặc định 6 tháng gần nhất nếu from / to là null. */
    BookingStatisticsResponse getBookingStatistics(LocalDate from, LocalDate to);

    /** Mặc định tháng hiện tại nếu from / to là null. */
    List<RoomUsageStatisticsResponse> getRoomUsage(LocalDate from, LocalDate to, int limit);
}