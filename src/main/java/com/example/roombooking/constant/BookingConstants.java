package com.example.roombooking.constant;

import com.example.roombooking.entity.Booking.BookingStatus;

import java.time.LocalTime;
import java.util.List;

public final class BookingConstants {

    private BookingConstants() {}

    // ===== Quy tắc thời gian đặt phòng =====
    public static final int MIN_DURATION_MINUTES = 30;
    public static final int MAX_DURATION_HOURS = 12;

    /** Giờ làm việc cho phép đặt phòng. */
    public static final LocalTime OPENING_TIME = LocalTime.of(6, 0);
    public static final LocalTime CLOSING_TIME = LocalTime.of(22, 0);

    /** Số giờ mở cửa mỗi ngày, dùng tính tỷ lệ sử dụng phòng. */
    public static final int OPEN_HOURS_PER_DAY = 16;

    /** Phải đặt trước tối thiểu bao nhiêu phút so với giờ bắt đầu. */
    public static final int MIN_ADVANCE_MINUTES = 0;

    /** Số ngày tối đa được đặt trước. */
    public static final int MAX_ADVANCE_DAYS = 365;

    // ===== Nhóm trạng thái =====
    /** Chỉ lịch đã duyệt mới chiếm chỗ (dùng kiểm tra trùng giờ). */
    public static final List<BookingStatus> BLOCKING_STATUSES = List.of(BookingStatus.APPROVED);

    /** Lịch hiển thị trên lịch tuần. */
    public static final List<BookingStatus> VISIBLE_ON_CALENDAR =
            List.of(BookingStatus.PENDING, BookingStatus.APPROVED);

    /** Lịch còn được phép hủy. */
    public static final List<BookingStatus> CANCELLABLE_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.APPROVED);

    /** Lịch còn được phép sửa / dời. */
    public static final List<BookingStatus> EDITABLE_STATUSES = List.of(BookingStatus.PENDING);

    // ===== Hành động trong lịch sử đặt phòng =====
    public static final String ACTION_CREATED = "CREATED";
    public static final String ACTION_APPROVED = "APPROVED";
    public static final String ACTION_REJECTED = "REJECTED";
    public static final String ACTION_CANCELLED = "CANCELLED";
    public static final String ACTION_RESCHEDULED = "RESCHEDULED";

    // ===== Mẫu thông báo (dùng với String.format) =====
    public static final String MSG_CREATED_TITLE = "Có lịch đặt phòng mới";
    public static final String MSG_CREATED_BODY = "%s vừa đăng ký phòng %s (%s). Vui lòng xem xét duyệt.";

    public static final String MSG_APPROVED_TITLE = "Lịch đặt phòng đã được duyệt";
    public static final String MSG_APPROVED_BODY = "Lịch \"%s\" tại phòng %s (%s) đã được duyệt.";

    public static final String MSG_REJECTED_TITLE = "Lịch đặt phòng bị từ chối";
    public static final String MSG_REJECTED_BODY = "Lịch \"%s\" tại phòng %s bị từ chối. Lý do: %s";

    public static final String MSG_CANCELLED_TITLE = "Lịch đặt phòng đã bị hủy";
    public static final String MSG_CANCELLED_BODY = "Lịch \"%s\" tại phòng %s (%s) đã bị hủy. Lý do: %s";

    public static final String MSG_RESCHEDULED_TITLE = "Lịch đặt phòng được dời";
    public static final String MSG_RESCHEDULED_BODY = "Lịch \"%s\" đã được dời sang %s và đang chờ duyệt lại.";

    // ===== Thông báo lỗi nghiệp vụ dùng chung =====
    public static final String ERR_ROOM_INACTIVE = "Phòng đang ngừng hoạt động";
    public static final String ERR_OVER_CAPACITY = "Số người tham dự vượt quá sức chứa của phòng (%d người)";
    public static final String ERR_CONFLICT = "Phòng đã có lịch được duyệt trong khoảng thời gian này";
    public static final String ERR_OUTSIDE_HOURS = "Chỉ được đặt phòng trong khung giờ %s - %s";
    public static final String ERR_TOO_SHORT = "Thời lượng đặt phòng tối thiểu là %d phút";
    public static final String ERR_TOO_LONG = "Thời lượng đặt phòng tối đa là %d giờ";
    public static final String ERR_NOT_EDITABLE = "Chỉ được thao tác với lịch đang ở trạng thái chờ duyệt";
    public static final String ERR_NOT_CANCELLABLE = "Lịch ở trạng thái hiện tại không thể hủy";
}