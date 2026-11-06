package com.example.roombooking.specification;

import com.example.roombooking.dto.booking.BookingSearchRequest;
import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.Booking.BookingStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class BookingSpecification {

    private BookingSpecification() {}

    /**
     * Tìm lịch đặt theo BookingSearchRequest.
     *
     * @param forcedUserId nếu khác null thì BỎ QUA req.userId và luôn lọc theo người này.
     *                     Service truyền id người đang đăng nhập khi họ là USER (không phải ADMIN),
     *                     để USER không thể xem lịch của người khác bằng cách tự điền userId.
     */
    public static Specification<Booking> search(BookingSearchRequest req, Long forcedUserId) {
        Long userId = forcedUserId != null ? forcedUserId : req.getUserId();

        List<Specification<Booking>> specs = new ArrayList<>();
        specs.add(fetchRoomAndUser());
        specs.add(hasRoom(req.getRoomId()));
        specs.add(hasUser(userId));
        specs.add(hasStatus(req.getStatus()));
        specs.add(startFrom(req.getFromDate()));
        specs.add(startBefore(req.getToDate()));
        specs.add(keyword(req.getKeyword()));
        return Specification.allOf(specs);
    }

    /**
     * Lịch hiển thị trên lịch tuần: bắt đầu trong [from, toExclusive), thuộc các trạng thái cho trước,
     * có thể lọc theo một phòng.
     */
    public static Specification<Booking> inRange(LocalDateTime from, LocalDateTime toExclusive,
                                                 Long roomId, Collection<BookingStatus> statuses) {
        List<Specification<Booking>> specs = new ArrayList<>();
        specs.add(fetchRoomAndUser());
        specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startTime"), from));
        specs.add((root, query, cb) -> cb.lessThan(root.get("startTime"), toExclusive));
        specs.add(hasRoom(roomId));
        specs.add(hasAnyStatus(statuses));
        return Specification.allOf(specs);
    }

    // ===================== Điều kiện đơn =====================

    public static Specification<Booking> hasRoom(Long roomId) {
        return (root, query, cb) -> roomId == null
                ? cb.conjunction()
                : cb.equal(root.get("room").get("id"), roomId);
    }

    public static Specification<Booking> hasUser(Long userId) {
        return (root, query, cb) -> userId == null
                ? cb.conjunction()
                : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Booking> hasStatus(BookingStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Booking> hasAnyStatus(Collection<BookingStatus> statuses) {
        return (root, query, cb) -> statuses == null || statuses.isEmpty()
                ? cb.conjunction()
                : root.get("status").in(statuses);
    }

    /** startTime >= đầu ngày fromDate. */
    public static Specification<Booking> startFrom(LocalDate fromDate) {
        return (root, query, cb) -> fromDate == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("startTime"), fromDate.atStartOfDay());
    }

    /** startTime < đầu ngày hôm sau toDate (tức là gồm cả ngày toDate). */
    public static Specification<Booking> startBefore(LocalDate toDate) {
        return (root, query, cb) -> toDate == null
                ? cb.conjunction()
                : cb.lessThan(root.get("startTime"), toDate.plusDays(1).atStartOfDay());
    }

    /** Tìm theo tiêu đề, mục đích, tên người đặt, mã hoặc tên phòng. */
    public static Specification<Booking> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String pattern = "%" + escape(keyword.trim().toLowerCase()) + "%";

            Join<Object, Object> room = root.join("room", JoinType.INNER);
            Join<Object, Object> user = root.join("user", JoinType.INNER);
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern, '\\'),
                    cb.like(cb.lower(root.get("purpose")), pattern, '\\'),
                    cb.like(cb.lower(user.get("fullName")), pattern, '\\'),
                    cb.like(cb.lower(room.get("code")), pattern, '\\'),
                    cb.like(cb.lower(room.get("name")), pattern, '\\'));
        };
    }

    /** Chỉ các lịch đang chờ duyệt (dùng cho trang duyệt của ADMIN). */
    public static Specification<Booking> pending() {
        return hasStatus(BookingStatus.PENDING);
    }

    // ===================== Tối ưu truy vấn =====================

    /**
     * Nạp sẵn room và user bằng JOIN FETCH để mapper không phải truy vấn thêm cho từng dòng (lỗi N+1).
     * Bỏ qua khi Spring Data chạy câu COUNT cho phân trang (kết quả kiểu Long).
     */
    private static Specification<Booking> fetchRoomAndUser() {
        return (root, query, cb) -> {
            Class<?> resultType = query.getResultType();
            if (resultType != Long.class && resultType != long.class) {
                root.fetch("room", JoinType.INNER);
                root.fetch("user", JoinType.INNER);
            }
            return cb.conjunction();
        };
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}