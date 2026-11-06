package com.example.roombooking.mapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.constant.BookingConstants;
import com.example.roombooking.dto.attachment.AttachmentResponse;
import com.example.roombooking.dto.booking.BookingConflictResponse;
import com.example.roombooking.dto.booking.BookingConflictResponse.ConflictItem;
import com.example.roombooking.dto.booking.BookingCreateRequest;
import com.example.roombooking.dto.booking.BookingDetailResponse;
import com.example.roombooking.dto.booking.BookingHistoryResponse;
import com.example.roombooking.dto.booking.BookingResponse;
import com.example.roombooking.dto.booking.BookingUpdateRequest;
import com.example.roombooking.dto.booking.WeeklyScheduleResponse;
import com.example.roombooking.dto.booking.WeeklyScheduleResponse.DaySchedule;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.Booking.BookingStatus;
import com.example.roombooking.entity.BookingAttachment;
import com.example.roombooking.entity.Room;
import com.example.roombooking.util.DateTimeUtils;

/**
 * Lưu ý: toDetailResponse và toHistory đọc các quan hệ LAZY (reviewedBy, attachments, uploadedBy),
 * nên phải được gọi trong một @Transactional ở service.
 */
@Component
public class BookingMapper {

    // ===================== Entity -> Response =====================

    public BookingResponse toResponse(Booking b) {
        if (b == null) return null;
        return new BookingResponse(
                b.getId(),
                b.getRoom().getId(), b.getRoom().getCode(), b.getRoom().getName(),
                b.getUser().getId(), b.getUser().getFullName(),
                b.getTitle(), b.getPurpose(), b.getAttendees(),
                b.getStartTime(), b.getEndTime(),
                b.getStatus(), b.getRejectReason(), b.getCreatedAt());
    }

    public List<BookingResponse> toResponseList(List<Booking> bookings) {
        return bookings.stream().map(this::toResponse).toList();
    }

    public BookingDetailResponse toDetailResponse(Booking b) {
        if (b == null) return null;
        Room room = b.getRoom();
        AppUser user = b.getUser();
        List<AttachmentResponse> attachments = b.getAttachments() == null
                ? List.of()
                : b.getAttachments().stream()
                        .sorted(Comparator.comparing((BookingAttachment a) -> a.getUploadedAt()))
                        .map(this::toAttachmentResponse)
                        .toList();

        return new BookingDetailResponse(
                b.getId(),
                room.getId(), room.getCode(), room.getName(), room.getCapacity(),
                user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getDepartment(),
                b.getTitle(), b.getPurpose(), b.getAttendees(),
                b.getStartTime(), b.getEndTime(),
                b.getStatus(), b.getRejectReason(),
                b.getReviewedBy() != null ? b.getReviewedBy().getFullName() : null,
                b.getReviewedAt(),
                b.getCancelReason(), b.getCancelledAt(),
                attachments,
                b.getCreatedAt(), b.getUpdatedAt());
    }

    public AttachmentResponse toAttachmentResponse(BookingAttachment a) {
        if (a == null) return null;
        Long bookingId = a.getBooking().getId();
        return new AttachmentResponse(
                a.getId(), bookingId,
                a.getOriginalFileName(), a.getContentType(), a.getFileSize(),
                a.getUploadSource(), a.getRelativePath(),
                ApiConstants.BOOKINGS + "/" + bookingId + "/attachments/" + a.getId() + "/download",
                a.getUploadedBy() != null ? a.getUploadedBy().getFullName() : null,
                a.getUploadedAt());
    }

    // ===================== Trùng giờ =====================

    public ConflictItem toConflictItem(Booking b) {
        return new ConflictItem(b.getId(), b.getTitle(), b.getUser().getFullName(),
                b.getStartTime(), b.getEndTime());
    }

    public List<ConflictItem> toConflictItems(List<Booking> bookings) {
        return bookings.stream().map(this::toConflictItem).toList();
    }

    /** Dùng cho API "kiểm tra trùng giờ" (không ném lỗi, chỉ báo kết quả). */
    public BookingConflictResponse toConflictResponse(List<Booking> conflicts) {
        boolean has = !conflicts.isEmpty();
        return new BookingConflictResponse(
                has,
                has ? BookingConstants.ERR_CONFLICT : "Phòng trống trong khoảng thời gian này",
                toConflictItems(conflicts));
    }

    // ===================== Lịch tuần =====================

    /**
     * Gom danh sách lịch theo từng ngày, luôn đủ 7 ngày từ weekStart (thứ Hai) đến weekEnd (Chủ nhật).
     * Ngày không có lịch trả danh sách rỗng. dayOfWeek là tên tiếng Việt ("Thứ Hai", ..., "Chủ nhật").
     *
     * @param room có thể null khi xem lịch của tất cả phòng
     */
    public WeeklyScheduleResponse toWeeklySchedule(LocalDate weekStart, LocalDate weekEnd,
                                                   Room room, List<Booking> bookings) {
        List<DaySchedule> days = new ArrayList<>();
        for (LocalDate d = weekStart; !d.isAfter(weekEnd); d = d.plusDays(1)) {
            final LocalDate day = d;
            List<BookingResponse> items = bookings.stream()
                    .filter(b -> b.getStartTime().toLocalDate().equals(day))
                    .sorted(Comparator.comparing((Booking b) -> b.getStartTime()))
                    .map(this::toResponse)
                    .toList();
            days.add(new DaySchedule(day, DateTimeUtils.vietnameseDayName(day.getDayOfWeek()), items));
        }
        return new WeeklyScheduleResponse(
                weekStart, weekEnd,
                room != null ? room.getId() : null,
                room != null ? room.getName() : null,
                days);
    }

    // ===================== Lịch sử =====================

    /**
     * Dựng lịch sử từ các cột có sẵn của Booking (chưa có bảng lịch sử riêng):
     * CREATED, APPROVED/REJECTED (reviewedAt), CANCELLED (cancelledAt).
     * Hạn chế: không dựng lại được các lần dời lịch (RESCHEDULED), và không biết ai bấm hủy
     * nên actorName của CANCELLED lấy theo chủ lịch.
     */
    public List<BookingHistoryResponse> toHistory(Booking b) {
        List<BookingHistoryResponse> history = new ArrayList<>();

        history.add(new BookingHistoryResponse(
                b.getId(), BookingConstants.ACTION_CREATED, BookingStatus.PENDING,
                b.getUser().getFullName(), b.getTitle(), b.getCreatedAt()));

        if (b.getReviewedAt() != null) {
            boolean rejected = b.getRejectReason() != null && !b.getRejectReason().isBlank();
            history.add(new BookingHistoryResponse(
                    b.getId(),
                    rejected ? BookingConstants.ACTION_REJECTED : BookingConstants.ACTION_APPROVED,
                    rejected ? BookingStatus.REJECTED : BookingStatus.APPROVED,
                    b.getReviewedBy() != null ? b.getReviewedBy().getFullName() : null,
                    rejected ? b.getRejectReason() : null,
                    b.getReviewedAt()));
        }

        if (b.getCancelledAt() != null) {
            history.add(new BookingHistoryResponse(
                    b.getId(), BookingConstants.ACTION_CANCELLED, BookingStatus.CANCELLED,
                    b.getUser().getFullName(), b.getCancelReason(), b.getCancelledAt()));
        }

        history.sort(Comparator.comparing((BookingHistoryResponse h) -> h.occurredAt()));
        return history;
    }

    // ===================== Request -> Entity =====================

    /** Gán người đặt và trạng thái PENDING; room và user do service truyền vào. */
    public Booking toEntity(BookingCreateRequest req, Room room, AppUser user) {
        return Booking.builder()
                .room(room)
                .user(user)
                .title(req.title().trim())
                .purpose(blankToNull(req.purpose()))
                .attendees(req.attendees())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .status(BookingStatus.PENDING)
                .build();
    }

    public void updateEntity(Booking b, BookingUpdateRequest req, Room room) {
        b.setRoom(room);
        b.setTitle(req.title().trim());
        b.setPurpose(blankToNull(req.purpose()));
        b.setAttendees(req.attendees());
        b.setStartTime(req.startTime());
        b.setEndTime(req.endTime());
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}