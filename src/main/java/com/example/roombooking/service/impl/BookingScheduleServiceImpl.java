package com.example.roombooking.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.roombooking.constant.BookingConstants;
import com.example.roombooking.dto.booking.BookingRescheduleRequest;
import com.example.roombooking.dto.booking.BookingResponse;
import com.example.roombooking.dto.booking.WeeklyScheduleResponse;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.Booking_;
import com.example.roombooking.entity.Booking.BookingStatus;
import com.example.roombooking.entity.Notification.NotificationType;
import com.example.roombooking.entity.Role;
import com.example.roombooking.entity.Room;
import com.example.roombooking.exception.BadRequestException;
import com.example.roombooking.exception.ForbiddenOperationException;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.mapper.BookingMapper;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.RoomRepository;
import com.example.roombooking.service.BookingConflictService;
import com.example.roombooking.service.BookingScheduleService;
import com.example.roombooking.service.NotificationService;
import com.example.roombooking.service.UserService;
import com.example.roombooking.specification.BookingSpecification;
import com.example.roombooking.util.DateTimeUtils;


@Service
@Transactional(readOnly = true)
public class BookingScheduleServiceImpl implements BookingScheduleService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final BookingMapper bookingMapper;
    private final UserService userService;
    private final BookingConflictService conflictService;
    private final NotificationService notificationService;

    @java.beans.ConstructorProperties({"bookingRepository", "roomRepository", "bookingMapper", "userService", "conflictService", "notificationService"})
    public BookingScheduleServiceImpl(
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

    // ===================== Lịch tuần =====================

    @Override
    public WeeklyScheduleResponse weekly(LocalDate date, Long roomId) {
        Room room = roomId == null ? null : roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("phòng", roomId));
        return buildWeek(date, room, null);
    }

    @Override
    public WeeklyScheduleResponse myWeekly(LocalDate date) {
        return buildWeek(date, null, userService.getCurrentUserEntity().getId());
    }

    private WeeklyScheduleResponse buildWeek(LocalDate date, Room room, Long userId) {
        LocalDate base = date != null ? date : DateTimeUtils.today();
        LocalDate monday = DateTimeUtils.startOfWeek(base);
        LocalDate sunday = DateTimeUtils.endOfWeek(base);

        Specification<Booking> spec = BookingSpecification.inRange(
                DateTimeUtils.startOfDay(monday), DateTimeUtils.endOfDayExclusive(sunday),
                room != null ? room.getId() : null, BookingConstants.VISIBLE_ON_CALENDAR);
        if (userId != null) spec = spec.and(BookingSpecification.hasUser(userId));

        List<Booking> bookings = bookingRepository.findAll(spec, 
            Sort.by(Sort.Direction.ASC, Booking_.START_TIME));
        return bookingMapper.toWeeklySchedule(monday, sunday, room, bookings);
    }

    // ===================== Dời lịch =====================

    @Override
    @Transactional
    public BookingResponse reschedule(Long bookingId, BookingRescheduleRequest req) {
        AppUser current = userService.getCurrentUserEntity();
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("lịch đặt", bookingId));

        boolean admin = current.hasRole(Role.ADMIN);
        boolean owner = b.getUser().getId().equals(current.getId());
        if (!owner && !admin)
            throw new ForbiddenOperationException("Bạn chỉ được dời lịch đặt của chính mình");
        if (!BookingConstants.CANCELLABLE_STATUSES.contains(b.getStatus()))
            throw new BadRequestException("Chỉ dời được lịch đang chờ duyệt hoặc đã duyệt");
        if (b.getEndTime().isBefore(DateTimeUtils.now()))
            throw new BadRequestException("Lịch đã diễn ra, không thể dời");

        Long targetRoomId = req.newRoomId() != null ? req.newRoomId() : b.getRoom().getId();
        Room room = roomRepository.findByIdForUpdate(targetRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("phòng", targetRoomId));
        conflictService.validateRules(room, req.newStartTime(), req.newEndTime(), b.getAttendees());
        conflictService.assertNoConflict(room.getId(), req.newStartTime(), req.newEndTime(), b.getId());

        // ADMIN dời lịch đã duyệt thì giữ nguyên trạng thái; còn lại quay về PENDING để duyệt lại
        boolean keepApproved = admin && b.getStatus() == BookingStatus.APPROVED;
        b.setRoom(room);
        b.setStartTime(req.newStartTime());
        b.setEndTime(req.newEndTime());
        if (!keepApproved) {
            b.setStatus(BookingStatus.PENDING);
            b.setReviewedBy(null);
            b.setReviewedAt(null);
            b.setRejectReason(null);
        }
        Booking saved = bookingRepository.save(b);

        String range = DateTimeUtils.formatRange(saved.getStartTime(), saved.getEndTime());
        String reasonText = req.reason() != null && !req.reason().isBlank() ? " Lý do: " + req.reason().trim() : "";
        if (owner) {
            notificationService.notifyAdmins(saved, NotificationType.BOOKING_RESCHEDULED,
                    BookingConstants.MSG_RESCHEDULED_TITLE,
                    String.format(BookingConstants.MSG_RESCHEDULED_BODY, saved.getTitle(), range) + reasonText,
                    current.getId());
        } else {
            String body = keepApproved
                    ? "Lịch \"" + saved.getTitle() + "\" đã được quản trị viên dời sang " + range + "." + reasonText
                    : String.format(BookingConstants.MSG_RESCHEDULED_BODY, saved.getTitle(), range) + reasonText;
            notificationService.notifyUser(saved.getUser(), saved, NotificationType.BOOKING_RESCHEDULED,
                    BookingConstants.MSG_RESCHEDULED_TITLE, body);
        }
        return bookingMapper.toResponse(saved);
    }
}