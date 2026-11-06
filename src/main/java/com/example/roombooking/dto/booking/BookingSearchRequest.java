package com.example.roombooking.dto.booking;

import com.example.roombooking.entity.Booking.BookingStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Tham số tìm kiếm lịch đặt (query string). */
@Getter
@Setter
public class BookingSearchRequest {

    private Long roomId;

    /** Chỉ ADMIN mới lọc theo người đặt; USER luôn bị ép về chính mình ở service. */
    private Long userId;

    private BookingStatus status;
    private String keyword;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    @Min(value = 0, message = "Số trang không được âm")
    private int page = 0;

    @Min(value = 1, message = "Kích thước trang tối thiểu là 1")
    @Max(value = 100, message = "Kích thước trang tối đa là 100")
    private int size = 10;

    private String sortBy = "startTime";
    private String sortDir = "desc";
}