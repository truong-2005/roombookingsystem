package com.example.roombooking.dto.dashboard;

import java.util.Map;

public record BookingStatisticsResponse(
        long total,
        long pending,
        long approved,
        long rejected,
        long cancelled,
        /** Số lịch theo tháng, khóa dạng "2026-10". */
        Map<String, Long> byMonth
) {}