package com.example.roombooking.service;

import com.example.roombooking.dto.booking.BookingDecisionRequest;
import com.example.roombooking.dto.booking.BookingResponse;
import com.example.roombooking.dto.booking.BookingSearchRequest;
import com.example.roombooking.dto.common.PageResponse;

public interface BookingApprovalService {

    BookingResponse approve(Long bookingId);

    BookingResponse reject(Long bookingId, String reason);

    /** Duyệt hoặc từ chối theo BookingDecisionRequest. */
    BookingResponse decide(Long bookingId, BookingDecisionRequest req);

    /** Danh sách lịch chờ duyệt (ADMIN). */
    PageResponse<BookingResponse> getPendingBookings(BookingSearchRequest req);
}