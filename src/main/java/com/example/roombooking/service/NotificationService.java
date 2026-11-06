package com.example.roombooking.service;

import java.util.Collection;

import com.example.roombooking.dto.notification.NotificationListResponse;
import com.example.roombooking.dto.notification.NotificationReadResponse;
import com.example.roombooking.dto.notification.NotificationResponse;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.Notification.NotificationType;

public interface NotificationService {

    NotificationListResponse getMyNotifications(Boolean unreadOnly, int page, int size);

    NotificationResponse getById(Long id);

    long countUnread();

    NotificationReadResponse markAsRead(Long id);

    NotificationReadResponse markSelectedAsRead(Collection<Long> ids);

    NotificationReadResponse markAllAsRead();

    void delete(Long id);

    /** Xóa tất cả thông báo đã đọc của mình, trả về số dòng đã xóa. */
    int deleteAllRead();

    // ===== Dùng nội bộ =====

    void notifyUser(AppUser recipient, Booking booking, NotificationType type, String title, String message);

    /** Gửi cho mọi ADMIN đang hoạt động, trừ excludeUserId (thường là người vừa thao tác). */
    void notifyAdmins(Booking booking, NotificationType type, String title, String message, Long excludeUserId);

    void purgeOld();
}