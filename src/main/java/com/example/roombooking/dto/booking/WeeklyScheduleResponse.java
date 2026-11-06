package com.example.roombooking.dto.booking;

import java.time.LocalDate;
import java.util.List;

public record WeeklyScheduleResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        Long roomId,
        String roomName,
        List<DaySchedule> days
) {
    public record DaySchedule(
            LocalDate date,
            String dayOfWeek,
            List<BookingResponse> bookings
    ) {}
}