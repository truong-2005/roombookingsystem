package com.example.roombooking.controller;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.dto.booking.BookingRescheduleRequest;
import com.example.roombooking.dto.booking.BookingResponse;
import com.example.roombooking.dto.booking.WeeklyScheduleResponse;
import com.example.roombooking.service.BookingScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(ApiConstants.BOOKINGS)
@Tag(name = "Booking schedule", description = "Lịch tuần và dời lịch đặt")
public class BookingScheduleController {

    private final BookingScheduleService scheduleService;

    public BookingScheduleController(BookingScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/schedule/weekly")
    @Operation(summary = "Lịch đặt theo tuần", description = "Lấy lịch từ thứ Hai đến Chủ nhật; có thể lọc theo phòng.")
    public WeeklyScheduleResponse weekly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long roomId) {
        return scheduleService.weekly(date, roomId);
    }

    @GetMapping("/schedule/my-weekly")
    @Operation(summary = "Lịch tuần của tôi")
    public WeeklyScheduleResponse myWeekly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return scheduleService.myWeekly(date);
    }

    @PatchMapping("/{bookingId}/reschedule")
    @Operation(summary = "Dời lịch đặt", description = "Chủ lịch hoặc ADMIN dời lịch; lịch được chuyển về trạng thái chờ duyệt.")
    public BookingResponse reschedule(@PathVariable Long bookingId,
            @Valid @RequestBody BookingRescheduleRequest request) {
        return scheduleService.reschedule(bookingId, request);
    }
}
