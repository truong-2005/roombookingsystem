package com.example.roombooking.service.impl;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.dto.notification.*;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.Notification;
import com.example.roombooking.entity.Notification.NotificationType;
import com.example.roombooking.entity.Role;
import com.example.roombooking.exception.BadRequestException;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.mapper.NotificationMapper;
import com.example.roombooking.repository.AppUserRepository;
import com.example.roombooking.repository.NotificationRepository;
import com.example.roombooking.service.EmailService;
import com.example.roombooking.service.NotificationService;
import com.example.roombooking.service.UserService;
import com.example.roombooking.util.DateTimeUtils;
import com.example.roombooking.util.PaginationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Slf4j
@Service
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository userRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final NotificationMapper notificationMapper;

    @java.beans.ConstructorProperties({"notificationRepository", "userRepository", "userService", "emailService", "notificationMapper"})
    public NotificationServiceImpl(
        NotificationRepository notificationRepository,
        AppUserRepository userRepository,
        UserService userService,
        EmailService emailService,
        NotificationMapper notificationMapper
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.emailService = emailService;
        this.notificationMapper = notificationMapper;
    }

    // ===================== Của tôi =====================

    @Override
    public NotificationListResponse getMyNotifications(Boolean unreadOnly, int page, int size) {
        Long userId = userService.getCurrentUserEntity().getId();
        Pageable pageable = PaginationUtils.build(page, size, "createdAt", "desc",
                ApiConstants.NOTIFICATION_SORT_FIELDS, "createdAt");
        Page<Notification> result = Boolean.TRUE.equals(unreadOnly)
                ? notificationRepository.findByUserIdAndRead(userId, false, pageable)
                : notificationRepository.findByUserId(userId, pageable);
        return notificationMapper.toListResponse(result, notificationRepository.countByUserIdAndReadFalse(userId));
    }

    @Override
    public NotificationResponse getById(Long id) {
        return notificationMapper.toResponse(getOwned(id));
    }

    @Override
    public long countUnread() {
        return notificationRepository.countByUserIdAndReadFalse(userService.getCurrentUserEntity().getId());
    }

    @Override
    @Transactional
    public NotificationReadResponse markAsRead(Long id) {
        Notification n = getOwned(id);
        int updated = 0;
        if (!n.isRead()) {
            n.markAsRead();
            notificationRepository.save(n);
            updated = 1;
        }
        return notificationMapper.toReadResponse(updated, countUnread());
    }

    @Override
    @Transactional
    public NotificationReadResponse markSelectedAsRead(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BadRequestException("Vui lòng chọn ít nhất một thông báo");
        Long userId = userService.getCurrentUserEntity().getId();
        int updated = notificationRepository.markAsReadByIds(userId, ids, DateTimeUtils.now());
        return notificationMapper.toReadResponse(updated, notificationRepository.countByUserIdAndReadFalse(userId));
    }

    @Override
    @Transactional
    public NotificationReadResponse markAllAsRead() {
        Long userId = userService.getCurrentUserEntity().getId();
        int updated = notificationRepository.markAllAsRead(userId, DateTimeUtils.now());
        return notificationMapper.toReadResponse(updated, 0);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        notificationRepository.delete(getOwned(id));
    }

    @Override
    @Transactional
    public int deleteAllRead() {
        return notificationRepository.deleteReadByUserId(userService.getCurrentUserEntity().getId());
    }

    // ===================== Dùng nội bộ =====================

    @Override
    @Transactional
    public void notifyUser(AppUser recipient, Booking booking, NotificationType type, String title, String message) {
        String safeTitle = cut(title, 200);
        String safeMessage = cut(message, 1000);
        notificationRepository.save(notificationMapper.toEntity(recipient, booking, type, safeTitle, safeMessage));
        emailService.sendBookingNotificationEmail(recipient.getEmail(), recipient.getFullName(), safeTitle, safeMessage);
    }

    @Override
    @Transactional
    public void notifyAdmins(Booking booking, NotificationType type, String title, String message, Long excludeUserId) {
        userRepository.findByRoles_NameAndEnabledTrue(Role.ADMIN).stream()
                .filter(admin -> !admin.getId().equals(excludeUserId))
                .forEach(admin -> notifyUser(admin, booking, type, title, message));
    }

    @Override
    @Scheduled(cron = "0 0 4 * * SUN")
    @Transactional
    public void purgeOld() {
        int n = notificationRepository.deleteOlderThan(DateTimeUtils.now().minusDays(90));
        log.info("Đã dọn {} thông báo cũ hơn 90 ngày", n);
    }

    // ===================== Helper =====================

    /** Chỉ lấy thông báo của chính người đang đăng nhập, id của người khác coi như không tồn tại. */
    private Notification getOwned(Long id) {
        Long userId = userService.getCurrentUserEntity().getId();
        return notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("thông báo", id));
    }

    private String cut(String s, int max) {
        return s == null ? "" : (s.length() <= max ? s : s.substring(0, max - 3) + "...");
    }
}