package com.example.roombooking.service;

import java.time.LocalDateTime;
import java.util.List;

import com.example.roombooking.dto.booking.BookingConflictResponse;
import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.Room;

public interface BookingConflictService {

    List<Booking> findConflicts(Long roomId, LocalDateTime start, LocalDateTime end, Long excludeBookingId);

    /** Kiểm tra không ném lỗi, dùng cho API "kiểm tra trùng giờ". */
    BookingConflictResponse check(Long roomId, LocalDateTime start, LocalDateTime end, Long excludeBookingId);

    /** Ném BookingConflictException (kèm danh sách lịch trùng) nếu bị trùng. */
    void assertNoConflict(Long roomId, LocalDateTime start, LocalDateTime end, Long excludeBookingId);

    /** Kiểm tra quy tắc đặt phòng: phòng hoạt động, giờ hợp lệ, thời lượng, sức chứa... */
    void validateRules(Room room, LocalDateTime start, LocalDateTime end, Integer attendees);
}