package com.example.roombooking.service;

import java.util.List;

import com.example.roombooking.dto.booking.BookingCancelRequest;
import com.example.roombooking.dto.booking.BookingCreateRequest;
import com.example.roombooking.dto.booking.BookingDetailResponse;
import com.example.roombooking.dto.booking.BookingHistoryResponse;
import com.example.roombooking.dto.booking.BookingResponse;
import com.example.roombooking.dto.booking.BookingSearchRequest;
import com.example.roombooking.dto.booking.BookingUpdateRequest;
import com.example.roombooking.dto.common.PageResponse;

public interface BookingService {

    /** USER chỉ thấy lịch của mình, ADMIN thấy tất cả. */
    PageResponse<BookingResponse> search(BookingSearchRequest req);

    BookingDetailResponse getById(Long id);

    BookingResponse create(BookingCreateRequest req);

    BookingResponse update(Long id, BookingUpdateRequest req);

    void delete(Long id);

    BookingResponse cancel(Long id, BookingCancelRequest req);

    List<BookingHistoryResponse> getHistory(Long id);
}