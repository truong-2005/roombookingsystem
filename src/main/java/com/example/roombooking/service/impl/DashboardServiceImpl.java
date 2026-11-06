package com.example.roombooking.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.roombooking.constant.BookingConstants;
import com.example.roombooking.dto.dashboard.AdminDashboardResponse;
import com.example.roombooking.dto.dashboard.BookingStatisticsResponse;
import com.example.roombooking.dto.dashboard.RoomUsageStatisticsResponse;
import com.example.roombooking.dto.dashboard.UserDashboardResponse;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.Booking.BookingStatus;
import com.example.roombooking.entity.Room;
import com.example.roombooking.exception.BadRequestException;
import com.example.roombooking.mapper.BookingMapper;
import com.example.roombooking.repository.AppUserRepository;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.NotificationRepository;
import com.example.roombooking.repository.RoomRepository;
import com.example.roombooking.service.DashboardService;
import com.example.roombooking.service.UserService;
import com.example.roombooking.util.DateTimeUtils;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final int MAX_RANGE_MONTHS = 60;

    private final AppUserRepository userRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final BookingMapper bookingMapper;

    @ConstructorProperties({"userRepository", "roomRepository", "bookingRepository", "notificationRepository",
            "userService", "bookingMapper"})
    public DashboardServiceImpl(AppUserRepository userRepository,
                                RoomRepository roomRepository,
                                BookingRepository bookingRepository,
                                NotificationRepository notificationRepository,
                                UserService userService,
                                BookingMapper bookingMapper) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.notificationRepository = notificationRepository;
        this.userService = userService;
        this.bookingMapper = bookingMapper;
    }

    // ===================== Admin =====================

    @Override
    public AdminDashboardResponse getAdminDashboard() {
        LocalDate today = DateTimeUtils.today();
        LocalDate monthStart = DateTimeUtils.startOfMonth(today);
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());

        long bookingsToday = bookingRepository.countByStartTimeGreaterThanEqualAndStartTimeLessThan(
                DateTimeUtils.startOfDay(today), DateTimeUtils.endOfDayExclusive(today));

        return new AdminDashboardResponse(
                userRepository.count(),
                roomRepository.count(),
                roomRepository.countByActive(true),
                bookingRepository.countByStatus(BookingStatus.PENDING),
                bookingsToday,
                getBookingStatistics(monthStart.minusMonths(5), monthEnd),
                getRoomUsage(monthStart, monthEnd, 5),
                bookingMapper.toResponseList(bookingRepository.findByStatusOrderByCreatedAtAsc(
                        BookingStatus.PENDING, PageRequest.of(0, 5))));
    }

    // ===================== User =====================

    @Override
    public UserDashboardResponse getUserDashboard() {
        AppUser user = userService.getCurrentUserEntity();
        Long id = user.getId();
        return new UserDashboardResponse(
                bookingRepository.countByUserId(id),
                bookingRepository.countByUserIdAndStatus(id, BookingStatus.PENDING),
                bookingRepository.countByUserIdAndStatus(id, BookingStatus.APPROVED),
                notificationRepository.countByUserIdAndReadFalse(id),
                bookingMapper.toResponseList(
                        bookingRepository.findByUserIdAndStatusInAndStartTimeAfterOrderByStartTimeAsc(
                                id, BookingConstants.VISIBLE_ON_CALENDAR, DateTimeUtils.now(), PageRequest.of(0, 5))));
    }

    // ===================== Thống kê lịch đặt =====================

    @Override
    public BookingStatisticsResponse getBookingStatistics(LocalDate from, LocalDate to) {
        LocalDate f = from != null ? from : DateTimeUtils.startOfMonth(DateTimeUtils.today()).minusMonths(5);
        LocalDate t = to != null ? to : DateTimeUtils.today().with(TemporalAdjusters.lastDayOfMonth());
        validateRange(f, t);
        LocalDateTime start = DateTimeUtils.startOfDay(f);
        LocalDateTime end = DateTimeUtils.endOfDayExclusive(t);

        Map<BookingStatus, Long> byStatus = new EnumMap<>(BookingStatus.class);
        for (Object[] row : bookingRepository.countGroupedByStatus(start, end))
            byStatus.put((BookingStatus) row[0], (Long) row[1]);

        // Điền sẵn mọi tháng trong khoảng (tháng không có lịch = 0) để biểu đồ liền mạch
        Map<String, Long> byMonth = new LinkedHashMap<>();
        for (LocalDate m = DateTimeUtils.startOfMonth(f); !m.isAfter(t); m = m.plusMonths(1))
            byMonth.put(m.format(DateTimeUtils.MONTH_KEY_FORMAT), 0L);
        for (LocalDateTime time : bookingRepository.findStartTimesBetween(start, end)) {
            byMonth.merge(DateTimeUtils.monthKey(time), 1L,
                    (current, increment) -> Objects.requireNonNull(current)
                            + Objects.requireNonNull(increment));
        }

        long pending = byStatus.getOrDefault(BookingStatus.PENDING, 0L);
        long approved = byStatus.getOrDefault(BookingStatus.APPROVED, 0L);
        long rejected = byStatus.getOrDefault(BookingStatus.REJECTED, 0L);
        long cancelled = byStatus.getOrDefault(BookingStatus.CANCELLED, 0L);
        return new BookingStatisticsResponse(pending + approved + rejected + cancelled,
                pending, approved, rejected, cancelled, byMonth);
    }

    // ===================== Mức sử dụng phòng =====================

    @Override
    public List<RoomUsageStatisticsResponse> getRoomUsage(LocalDate from, LocalDate to, int limit) {
        LocalDate today = DateTimeUtils.today();
        LocalDate f = from != null ? from : DateTimeUtils.startOfMonth(today);
        LocalDate t = to != null ? to : today.with(TemporalAdjusters.lastDayOfMonth());
        validateRange(f, t);
        LocalDateTime start = DateTimeUtils.startOfDay(f);
        LocalDateTime end = DateTimeUtils.endOfDayExclusive(t);

        List<Object[]> grouped = bookingRepository.countGroupedByRoom(
                start, end, PageRequest.of(0, Math.min(Math.max(limit, 1), 50)));
        if (grouped.isEmpty()) return List.of();

        Map<Long, Long> totals = new LinkedHashMap<>();   // giữ thứ tự: phòng nhiều lịch nhất lên đầu
        for (Object[] row : grouped) totals.put((Long) row[0], (Long) row[1]);

        Map<Long, Room> rooms = roomRepository.findAllById(totals.keySet()).stream()
                .collect(Collectors.toMap(room -> Objects.requireNonNull(room).getId(), Function.identity()));
        Map<Long, List<Booking>> approvedByRoom = bookingRepository.findApprovedBetween(start, end).stream()
                .filter(b -> totals.containsKey(b.getRoom().getId()))
                .collect(Collectors.groupingBy(b -> b.getRoom().getId()));

        double availableHours = (ChronoUnit.DAYS.between(f, t) + 1) * (double) BookingConstants.OPEN_HOURS_PER_DAY;

        List<RoomUsageStatisticsResponse> result = new ArrayList<>();
        totals.forEach((roomId, total) -> {
            Room room = rooms.get(roomId);
            if (room == null) return;
            List<Booking> approved = approvedByRoom.getOrDefault(roomId, List.of());
            double hours = approved.stream()
                    .mapToDouble(b -> DateTimeUtils.hoursBetween(b.getStartTime(), b.getEndTime())).sum();
            double percent = availableHours <= 0 ? 0 : Math.min(100.0, hours / availableHours * 100.0);
            result.add(new RoomUsageStatisticsResponse(roomId, room.getCode(), room.getName(),
                    total, approved.size(), round1(hours), round1(percent)));
        });
        return result;
    }

    // ===================== Helper =====================

    private void validateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw new BadRequestException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        if (ChronoUnit.MONTHS.between(from, to) > MAX_RANGE_MONTHS)
            throw new BadRequestException("Khoảng thống kê tối đa " + MAX_RANGE_MONTHS + " tháng");
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
