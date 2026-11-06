package com.example.roombooking.specification;

import com.example.roombooking.constant.BookingConstants;
import com.example.roombooking.dto.room.RoomSearchRequest;
import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.Room;
import com.example.roombooking.entity.Room.RoomType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class RoomSpecification {

    private RoomSpecification() {}

    /** Tìm phòng theo các điều kiện trong RoomSearchRequest. */
    public static Specification<Room> search(RoomSearchRequest req) {
        List<Specification<Room>> specs = new ArrayList<>();
        specs.add(keyword(req.getKeyword()));
        specs.add(hasType(req.getType()));
        specs.add(minCapacity(req.getMinCapacity()));
        specs.add(isActive(req.getActive()));
        return Specification.allOf(specs);
    }

    /** Tìm theo mã, tên hoặc vị trí (không phân biệt hoa thường). */
    public static Specification<Room> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String pattern = "%" + escape(keyword.trim().toLowerCase()) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("code")), pattern, '\\'),
                    cb.like(cb.lower(root.get("name")), pattern, '\\'),
                    cb.like(cb.lower(root.get("location")), pattern, '\\'));
        };
    }

    public static Specification<Room> hasType(RoomType type) {
        return (root, query, cb) -> type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    public static Specification<Room> minCapacity(Integer minCapacity) {
        return (root, query, cb) -> minCapacity == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("capacity"), minCapacity);
    }

    public static Specification<Room> isActive(Boolean active) {
        return (root, query, cb) -> active == null ? cb.conjunction() : cb.equal(root.get("active"), active);
    }

    /**
     * Chỉ lấy phòng còn trống trong khoảng [start, end): không có lịch đã duyệt nào giao nhau.
     * Giao nhau khi start_cũ < end_mới VÀ end_cũ > start_mới.
     * Ghép với search(): RoomSpecification.search(req).and(RoomSpecification.freeBetween(s, e))
     */
    public static Specification<Room> freeBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null || end == null) return cb.conjunction();

            Subquery<Long> sub = query.subquery(Long.class);
            Root<Booking> b = sub.from(Booking.class);
            sub.select(b.get("id")).where(
                    cb.equal(b.get("room"), root),
                    b.get("status").in(BookingConstants.BLOCKING_STATUSES),
                    cb.lessThan(b.get("startTime"), end),
                    cb.greaterThan(b.get("endTime"), start));

            Predicate hasConflict = cb.exists(sub);
            return cb.not(hasConflict);
        };
    }

    /** Escape ký tự đặc biệt của LIKE để người dùng gõ % hoặc _ không bị hiểu là ký tự đại diện. */
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}