package com.example.roombooking.service;

import java.time.LocalDate;

import com.example.roombooking.dto.booking.BookingRescheduleRequest;
import com.example.roombooking.dto.booking.BookingResponse;
import com.example.roombooking.dto.booking.WeeklyScheduleResponse;

public interface BookingScheduleService {

    /** Lịch tuần (thứ Hai đến Chủ nhật) của một phòng hoặc tất cả phòng. */
    WeeklyScheduleResponse weekly(LocalDate date, Long roomId);

    /** Lịch tuần chỉ gồm các lịch của chính người đang đăng nhập. */
    WeeklyScheduleResponse myWeekly(LocalDate date);

    BookingResponse reschedule(Long bookingId, BookingRescheduleRequest req);
}