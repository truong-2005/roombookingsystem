package com.example.roombooking.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.constant.BookingConstants;
import com.example.roombooking.dto.booking.BookingDecisionRequest;
import com.example.roombooking.dto.booking.BookingResponse;
import com.example.roombooking.dto.booking.BookingSearchRequest;
import com.example.roombooking.dto.common.PageResponse;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.Booking.BookingStatus;
import com.example.roombooking.entity.Notification.NotificationType;
import com.example.roombooking.exception.BadRequestException;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.mapper.BookingMapper;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.RoomRepository;
import com.example.roombooking.service.BookingApprovalService;
import com.example.roombooking.service.BookingConflictService;
import com.example.roombooking.service.NotificationService;
import com.example.roombooking.service.UserService;
import com.example.roombooking.specification.BookingSpecification;
import com.example.roombooking.util.DateTimeUtils;
import com.example.roombooking.util.PaginationUtils;


@Service
@Transactional(readOnly = true)
public class BookingApprovalServiceImpl implements BookingApprovalService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final BookingMapper bookingMapper;
    private final UserService userService;
    private final BookingConflictService conflictService;
    private final NotificationService notificationService;

    @java.beans.ConstructorProperties({"bookingRepository", "roomRepository", "bookingMapper", "userService", "conflictService", "notificationService"})
    public BookingApprovalServiceImpl(
        BookingRepository bookingRepository,
        RoomRepository roomRepository,
        BookingMapper bookingMapper,
        UserService userService,
        BookingConflictService conflictService,
        NotificationService notificationService
    ) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.bookingMapper = bookingMapper;
        this.userService = userService;
        this.conflictService = conflictService;
        this.notificationService = notificationService;
    }
    
    @Override
    @Transactional
    public BookingResponse approve(Long bookingId) {
        AppUser admin = userService.getCurrentUserEntity();
        Booking b = getPending(bookingId, "duyệt");

        // Khóa phòng rồi kiểm tra lại trùng giờ: có thể đã có lịch khác được duyệt trước
        roomRepository.findByIdForUpdate(b.getRoom().getId());
        if (!b.getRoom().isActive()) throw new BadRequestException(BookingConstants.ERR_ROOM_INACTIVE);
        if (b.getEndTime().isBefore(DateTimeUtils.now()))
            throw new BadRequestException("Lịch đã quá hạn, không thể duyệt");
        conflictService.assertNoConflict(b.getRoom().getId(), b.getStartTime(), b.getEndTime(), b.getId());

        b.setStatus(BookingStatus.APPROVED);
        b.setRejectReason(null);
        b.setReviewedBy(admin);
        b.setReviewedAt(DateTimeUtils.now());
        Booking saved = bookingRepository.save(b);

        notificationService.notifyUser(saved.getUser(), saved, NotificationType.BOOKING_APPROVED,
                BookingConstants.MSG_APPROVED_TITLE,
                String.format(BookingConstants.MSG_APPROVED_BODY, saved.getTitle(), saved.getRoom().getCode(),
                        DateTimeUtils.formatRange(saved.getStartTime(), saved.getEndTime())));
        return bookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse reject(Long bookingId, String reason) {
        if (reason == null || reason.isBlank())
            throw new BadRequestException("Vui lòng nhập lý do từ chối");
        AppUser admin = userService.getCurrentUserEntity();
        Booking b = getPending(bookingId, "từ chối");

        b.setStatus(BookingStatus.REJECTED);
        b.setRejectReason(reason.trim());
        b.setReviewedBy(admin);
        b.setReviewedAt(DateTimeUtils.now());
        Booking saved = bookingRepository.save(b);

        notificationService.notifyUser(saved.getUser(), saved, NotificationType.BOOKING_REJECTED,
                BookingConstants.MSG_REJECTED_TITLE,
                String.format(BookingConstants.MSG_REJECTED_BODY, saved.getTitle(), saved.getRoom().getCode(),
                        saved.getRejectReason()));
        return bookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse decide(Long bookingId, BookingDecisionRequest req) {
        return Boolean.TRUE.equals(req.approved()) 
            ? approve(bookingId)
            : reject(bookingId, req.reason());
    }

    @Override
    public PageResponse<BookingResponse> getPendingBookings(BookingSearchRequest req) {
        req.setStatus(BookingStatus.PENDING);
        Pageable pageable = PaginationUtils.build(req.getPage(), req.getSize(), req.getSortBy(), req.getSortDir(),
                ApiConstants.BOOKING_SORT_FIELDS, "startTime");
        Page<Booking> page = bookingRepository.findAll(BookingSpecification.search(req, null), pageable);
        return PaginationUtils.toPageResponse(page, bookingMapper::toResponse);
    }

    private Booking getPending(Long id, String action) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("lịch đặt", id));
        if (b.getStatus() != BookingStatus.PENDING)
            throw new BadRequestException("Chỉ " + action + " được lịch đang ở trạng thái chờ duyệt");
        return b;
    }
}
