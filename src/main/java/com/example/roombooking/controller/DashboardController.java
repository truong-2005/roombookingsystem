package com.example.roombooking.controller;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.dto.dashboard.AdminDashboardResponse;
import com.example.roombooking.dto.dashboard.BookingStatisticsResponse;
import com.example.roombooking.dto.dashboard.RoomUsageStatisticsResponse;
import com.example.roombooking.dto.dashboard.UserDashboardResponse;
import com.example.roombooking.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.DASHBOARD)
@Validated
@Tag(name = "Dashboard", description = "Tổng quan, thống kê đặt phòng và mức sử dụng phòng")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bảng điều khiển quản trị viên")
    public AdminDashboardResponse getAdminDashboard() {
        return dashboardService.getAdminDashboard();
    }

    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Bảng điều khiển người dùng")
    public UserDashboardResponse getUserDashboard() {
        return dashboardService.getUserDashboard();
    }

    @GetMapping("/booking-statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thống kê lịch đặt", description = "Mặc định trả thống kê sáu tháng gần nhất nếu không truyền from/to.")
    public BookingStatisticsResponse getBookingStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardService.getBookingStatistics(from, to);
    }

    @GetMapping("/room-usage")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thống kê mức sử dụng phòng", description = "Mặc định dùng tháng hiện tại nếu không truyền from/to.")
    public List<RoomUsageStatisticsResponse> getRoomUsage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return dashboardService.getRoomUsage(from, to, limit);
    }
}
