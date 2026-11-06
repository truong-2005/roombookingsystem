package com.example.roombooking.controller;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.dto.booking.BookingDecisionRequest;
import com.example.roombooking.dto.booking.BookingResponse;
import com.example.roombooking.dto.booking.BookingSearchRequest;
import com.example.roombooking.dto.common.PageResponse;
import com.example.roombooking.service.BookingApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.BOOKINGS)
@Tag(name = "Booking approval", description = "Quản trị viên duyệt hoặc từ chối lịch đặt")
public class BookingApprovalController {

    private final BookingApprovalService approvalService;

    public BookingApprovalController(BookingApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách lịch chờ duyệt", description = "Chỉ ADMIN được xem các lịch đang chờ duyệt.")
    public PageResponse<BookingResponse> getPendingBookings(
            @Valid @ParameterObject @ModelAttribute BookingSearchRequest request) {
        return approvalService.getPendingBookings(request);
    }

    @PostMapping("/{bookingId}/approve")
    @Operation(summary = "Duyệt lịch đặt", description = "Chuyển lịch chờ duyệt sang trạng thái đã duyệt.")
    public BookingResponse approve(@PathVariable Long bookingId) {
        return approvalService.approve(bookingId);
    }

    @PostMapping("/{bookingId}/reject")
    @Operation(summary = "Từ chối lịch đặt", description = "Từ chối lịch đang chờ duyệt; lý do là bắt buộc.")
    public BookingResponse reject(@PathVariable Long bookingId, @RequestParam String reason) {
        return approvalService.reject(bookingId, reason);
    }

    @PostMapping("/{bookingId}/decision")
    @Operation(summary = "Duyệt hoặc từ chối lịch đặt", description = "Dùng approved=true để duyệt hoặc false để từ chối kèm reason.")
    public BookingResponse decide(@PathVariable Long bookingId, @Valid @RequestBody BookingDecisionRequest request) {
        return approvalService.decide(bookingId, request);
    }
}
