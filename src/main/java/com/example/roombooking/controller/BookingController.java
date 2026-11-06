package com.example.roombooking.controller;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.dto.booking.BookingCancelRequest;
import com.example.roombooking.dto.booking.BookingConflictResponse;
import com.example.roombooking.dto.booking.BookingCreateRequest;
import com.example.roombooking.dto.booking.BookingDetailResponse;
import com.example.roombooking.dto.booking.BookingHistoryResponse;
import com.example.roombooking.dto.booking.BookingResponse;
import com.example.roombooking.dto.booking.BookingSearchRequest;
import com.example.roombooking.dto.booking.BookingUpdateRequest;
import com.example.roombooking.dto.common.PageResponse;
import com.example.roombooking.service.BookingService;
import com.example.roombooking.service.BookingConflictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.BOOKINGS)
@Tag(name = "Bookings", description = "Tạo, tra cứu, cập nhật và hủy lịch đặt phòng")
public class BookingController {

    private final BookingService bookingService;
    private final BookingConflictService conflictService;

    public BookingController(BookingService bookingService, BookingConflictService conflictService) {
        this.bookingService = bookingService;
        this.conflictService = conflictService;
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm lịch đặt", description = "Người dùng chỉ xem lịch của mình; ADMIN có thể xem tất cả lịch.")
    public PageResponse<BookingResponse> search(@Valid @ParameterObject @ModelAttribute BookingSearchRequest request) {
        return bookingService.search(request);
    }

    @GetMapping("/conflicts")
    @Operation(summary = "Kiểm tra lịch bị trùng", description = "Kiểm tra các lịch đã duyệt hoặc đang chờ duyệt trong cùng phòng và khoảng thời gian.")
    public BookingConflictResponse checkConflicts(
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Long excludeBookingId) {
        return conflictService.check(roomId, startTime, endTime, excludeBookingId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết lịch đặt")
    public BookingDetailResponse getById(@PathVariable Long id) {
        return bookingService.getById(id);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Lịch sử trạng thái của lịch đặt")
    public List<BookingHistoryResponse> getHistory(@PathVariable Long id) {
        return bookingService.getHistory(id);
    }

    @PostMapping
    @Operation(summary = "Tạo lịch đặt", description = "Lịch mới ở trạng thái chờ duyệt; người đặt được lấy từ access token.")
    public BookingResponse create(@Valid @RequestBody BookingCreateRequest request) {
        return bookingService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật lịch đặt", description = "Chủ lịch hoặc ADMIN có thể sửa lịch còn cho phép chỉnh sửa.")
    public BookingResponse update(@PathVariable Long id, @Valid @RequestBody BookingUpdateRequest request) {
        return bookingService.update(id, request);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Hủy lịch đặt", description = "Chủ lịch hoặc ADMIN hủy lịch bằng cách gửi lý do.")
    public BookingResponse cancel(@PathVariable Long id, @Valid @RequestBody BookingCancelRequest request) {
        return bookingService.cancel(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa lịch đặt", description = "Chủ lịch hoặc ADMIN có thể xóa lịch theo quy tắc trạng thái nghiệp vụ.")
    public void delete(@PathVariable Long id) {
        bookingService.delete(id);
    }
}
