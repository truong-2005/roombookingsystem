package com.example.roombooking.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.roombooking.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    /** read = true: đã đọc, read = false: chưa đọc. */
    Page<Notification> findByUserIdAndRead(Long userId, boolean read, Pageable pageable);

    /** Tìm theo cả id thông báo và người nhận, tránh đọc hoặc sửa thông báo của người khác. */
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadFalse(Long userId);

    /** Đánh dấu tất cả đã đọc, trả về số dòng được cập nhật. */
    @Modifying(clearAutomatically = true)
    @Query("""
            update Notification n set n.read = true, n.readAt = :now
            where n.user.id = :userId and n.read = false
            """)
    int markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Đánh dấu đã đọc theo danh sách id, chỉ áp dụng cho thông báo của chính người đó. */
    @Modifying(clearAutomatically = true)
    @Query("""
            update Notification n set n.read = true, n.readAt = :now
            where n.user.id = :userId and n.read = false and n.id in :ids
            """)
    int markAsReadByIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids,
                        @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from Notification n where n.user.id = :userId and n.read = true")
    int deleteReadByUserId(@Param("userId") Long userId);

    /** Xóa toàn bộ thông báo của một người dùng (trước khi xóa tài khoản). */
    @Modifying
    @Query("delete from Notification n where n.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    /** Xóa các thông báo tham chiếu tới một lịch đặt (trước khi xóa lịch). */
    @Modifying
    @Query("delete from Notification n where n.booking.id = :bookingId")
    int deleteByBookingId(@Param("bookingId") Long bookingId);

    /** Dọn thông báo cũ (gọi từ job định kỳ). */
    @Modifying
    @Query("delete from Notification n where n.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}