package com.example.roombooking.service.impl;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.constant.BookingConstants;
import com.example.roombooking.dto.booking.*;
import com.example.roombooking.dto.common.PageResponse;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.Booking.BookingStatus;
import com.example.roombooking.entity.Notification.NotificationType;
import com.example.roombooking.entity.Role;
import com.example.roombooking.entity.Room;
import com.example.roombooking.exception.BadRequestException;
import com.example.roombooking.exception.ForbiddenOperationException;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.mapper.BookingMapper;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.NotificationRepository;
import com.example.roombooking.repository.RoomRepository;
import com.example.roombooking.service.*;
import com.example.roombooking.specification.BookingSpecification;
import com.example.roombooking.util.DateTimeUtils;
import com.example.roombooking.util.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final NotificationRepository notificationRepository;
    private final BookingMapper bookingMapper;
    private final UserService userService;
    private final BookingConflictService conflictService;
    private final BookingAttachmentService attachmentService;
    private final NotificationService notificationService;

    @java.beans.ConstructorProperties({"bookingRepository", "roomRepository", "notificationRepository", "bookingMapper", "userService", "conflictService", "attachmentService", "notificationService"})
    public BookingServiceImpl(
        BookingRepository bookingRepository,
        RoomRepository roomRepository,
        NotificationRepository notificationRepository,
        BookingMapper bookingMapper,
        UserService userService,
        BookingConflictService conflictService,
        BookingAttachmentService attachmentService,
        NotificationService notificationService
    ) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.notificationRepository = notificationRepository;
        this.bookingMapper = bookingMapper;
        this.userService = userService;
        this.conflictService = conflictService;
        this.attachmentService = attachmentService;
        this.notificationService = notificationService;
    }

    // ===================== Xem =====================

    @Override
    public PageResponse<BookingResponse> search(BookingSearchRequest req) {
        AppUser current = userService.getCurrentUserEntity();
        // USER bị ép về chính mình, bất kể có tự điền userId hay không
        Long forcedUserId = current.hasRole(Role.ADMIN) ? null : current.getId();
        Pageable pageable = PaginationUtils.build(req.getPage(), req.getSize(), req.getSortBy(), req.getSortDir(),
                ApiConstants.BOOKING_SORT_FIELDS, "startTime");
        Page<Booking> page = bookingRepository.findAll(BookingSpecification.search(req, forcedUserId), pageable);
        return PaginationUtils.toPageResponse(page, bookingMapper::toResponse);
    }

    @Override
    public BookingDetailResponse getById(Long id) {
        Booking b = bookingRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("lịch đặt", id));
        assertOwnerOrAdmin(b, userService.getCurrentUserEntity());
        return bookingMapper.toDetailResponse(b);
    }

    @Override
    public List<BookingHistoryResponse> getHistory(Long id) {
        Booking b = getEntity(id);
        assertOwnerOrAdmin(b, userService.getCurrentUserEntity());
        return bookingMapper.toHistory(b);
    }

    // ===================== Thêm =====================

    @Override
    @Transactional
    public BookingResponse create(BookingCreateRequest req) {
        AppUser user = userService.getCurrentUserEntity();
        // Khóa dòng phòng trước khi kiểm tra trùng giờ để hai người cùng đặt một phòng phải xếp hàng
        Room room = lockRoom(req.roomId());
        conflictService.validateRules(room, req.startTime(), req.endTime(), req.attendees());
        conflictService.assertNoConflict(room.getId(), req.startTime(), req.endTime(), null);

        Booking booking = bookingRepository.save(bookingMapper.toEntity(req, room, user));

        notificationService.notifyAdmins(booking, NotificationType.BOOKING_CREATED,
                BookingConstants.MSG_CREATED_TITLE,
                String.format(BookingConstants.MSG_CREATED_BODY, user.getFullName(), room.getCode(),
                        DateTimeUtils.formatRange(booking.getStartTime(), booking.getEndTime())),
                user.getId());
        return bookingMapper.toResponse(booking);
    }

    // ===================== Sửa =====================

    @Override
    @Transactional
    public BookingResponse update(Long id, BookingUpdateRequest req) {
        AppUser current = userService.getCurrentUserEntity();
        Booking b = getEntity(id);
        assertOwnerOrAdmin(b, current);
        if (!BookingConstants.EDITABLE_STATUSES.contains(b.getStatus()))
            throw new BadRequestException(BookingConstants.ERR_NOT_EDITABLE);

        Room room = lockRoom(req.roomId());
        conflictService.validateRules(room, req.startTime(), req.endTime(), req.attendees());
        conflictService.assertNoConflict(room.getId(), req.startTime(), req.endTime(), b.getId());

        bookingMapper.updateEntity(b, req, room);
        return bookingMapper.toResponse(bookingRepository.save(b));
    }

    // ===================== Hủy =====================

    @Override
    @Transactional
    public BookingResponse cancel(Long id, BookingCancelRequest req) {
        AppUser current = userService.getCurrentUserEntity();
        Booking b = getEntity(id);
        assertOwnerOrAdmin(b, current);
        if (!BookingConstants.CANCELLABLE_STATUSES.contains(b.getStatus()))
            throw new BadRequestException(BookingConstants.ERR_NOT_CANCELLABLE);
        if (b.getEndTime().isBefore(DateTimeUtils.now()))
            throw new BadRequestException("Lịch đã diễn ra, không thể hủy");

        b.setStatus(BookingStatus.CANCELLED);
        b.setCancelReason(req.reason().trim());
        b.setCancelledAt(DateTimeUtils.now());
        Booking saved = bookingRepository.save(b);

        String message = String.format(BookingConstants.MSG_CANCELLED_BODY, saved.getTitle(),
                saved.getRoom().getCode(),
                DateTimeUtils.formatRange(saved.getStartTime(), saved.getEndTime()), saved.getCancelReason());
        if (saved.getUser().getId().equals(current.getId())) {
            notificationService.notifyAdmins(saved, NotificationType.BOOKING_CANCELLED,
                    BookingConstants.MSG_CANCELLED_TITLE, message, current.getId());
        } else {
            notificationService.notifyUser(saved.getUser(), saved, NotificationType.BOOKING_CANCELLED,
                    BookingConstants.MSG_CANCELLED_TITLE, message);
        }
        return bookingMapper.toResponse(saved);
    }

    // ===================== Xóa =====================

    @Override
    @Transactional
    public void delete(Long id) {
        AppUser current = userService.getCurrentUserEntity();
        Booking b = getEntity(id);
        if (!current.hasRole(Role.ADMIN)) {
            assertOwnerOrAdmin(b, current);
            if (b.getStatus() == BookingStatus.APPROVED)
                throw new BadRequestException("Lịch đã duyệt không thể xóa, hãy hủy lịch trước");
        }
        attachmentService.deleteFilesOfBooking(b);       // xóa file vật lý
        notificationRepository.deleteByBookingId(id);    // thông báo tham chiếu tới lịch này
        bookingRepository.delete(b);
    }

    // ===================== Helper =====================

    private Booking getEntity(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("lịch đặt", id));
    }

    private Room lockRoom(Long roomId) {
        return roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("phòng", roomId));
    }

    private void assertOwnerOrAdmin(Booking b, AppUser current) {
        if (!b.getUser().getId().equals(current.getId()) && !current.hasRole(Role.ADMIN))
            throw new ForbiddenOperationException("Bạn chỉ được thao tác với lịch đặt của chính mình");
    }
}