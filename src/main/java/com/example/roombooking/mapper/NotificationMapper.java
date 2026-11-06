package com.example.roombooking.mapper;

import com.example.roombooking.dto.common.PageResponse;
import com.example.roombooking.dto.notification.NotificationListResponse;
import com.example.roombooking.dto.notification.NotificationReadResponse;
import com.example.roombooking.dto.notification.NotificationResponse;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.Notification;
import com.example.roombooking.entity.Notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification n) {
        if (n == null) return null;
        return new NotificationResponse(
                n.getId(),
                n.getBooking() != null ? n.getBooking().getId() : null,
                n.getType(), n.getTitle(), n.getMessage(),
                n.isRead(), n.getReadAt(), n.getCreatedAt());
    }

    /** Danh sách thông báo kèm số chưa đọc (để FE hiện chấm đỏ trên chuông). */
    public NotificationListResponse toListResponse(Page<Notification> page, long unreadCount) {
        PageResponse<NotificationResponse> content = PageResponse.of(page.map(this::toResponse));
        return new NotificationListResponse(unreadCount, content);
    }

    public NotificationReadResponse toReadResponse(int updatedCount, long unreadCount) {
        String message = updatedCount > 0
                ? "Đã đánh dấu " + updatedCount + " thông báo là đã đọc"
                : "Không có thông báo nào cần cập nhật";
        return new NotificationReadResponse(updatedCount, unreadCount, message);
    }

    /** Tạo thông báo mới; title và message đã được service ghép từ mẫu trong BookingConstants. */
    public Notification toEntity(AppUser recipient, Booking booking, NotificationType type,
                                 String title, String message) {
        return Notification.builder()
                .user(recipient)
                .booking(booking)
                .type(type)
                .title(title)
                .message(message)
                .build();
    }
}