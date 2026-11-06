package com.example.roombooking.repository;

import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.Booking.BookingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    // ===================== Chi tiết =====================

    /** Nạp đủ room, user, người duyệt, file đính kèm trong một lần truy vấn. */
    @EntityGraph(attributePaths = {"room", "user", "reviewedBy", "attachments", "attachments.uploadedBy"})
    Optional<Booking> findWithDetailsById(Long id);

    // ===================== Kiểm tra trùng giờ =====================

    /**
     * Lịch trùng giờ: start_cũ < end_mới VÀ end_cũ > start_mới (đặt sát nhau không tính là trùng).
     * excludeId dùng khi sửa / dời / duyệt để bỏ qua chính lịch đang xử lý.
     * Phải gọi sau RoomRepository.findByIdForUpdate để tránh hai người cùng lọt qua.
     */
    @Query("""
            select b from Booking b
            join fetch b.user
            where b.room.id = :roomId
              and b.status in :statuses
              and b.startTime < :endTime
              and b.endTime > :startTime
              and b.id <> :excludeId
            order by b.startTime
            """)
    List<Booking> findConflicts(@Param("roomId") Long roomId,
                                @Param("statuses") Collection<BookingStatus> statuses,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime,
                                @Param("excludeId") Long excludeId);

    /** Dùng khi tạo mới (chưa có id để loại trừ). */
    default List<Booking> findConflicts(Long roomId, Collection<BookingStatus> statuses,
                                        LocalDateTime startTime, LocalDateTime endTime) {
        return findConflicts(roomId, statuses, startTime, endTime, -1L);
    }

    // ===================== Ràng buộc khi xóa =====================

    boolean existsByRoomId(Long roomId);

    boolean existsByUserId(Long userId);

    /** Phòng còn lịch chưa diễn ra (đang chờ hoặc đã duyệt) thì không nên cho ngừng / xóa. */
    boolean existsByRoomIdAndStatusInAndEndTimeAfter(Long roomId, Collection<BookingStatus> statuses,
                                                     LocalDateTime time);

    // ===================== Danh sách phục vụ trang chủ / dashboard =====================

    /** Lịch sắp tới của một người. Truyền PageRequest.of(0, n) để giới hạn số dòng. */
    @EntityGraph(attributePaths = {"room", "user"})
    List<Booking> findByUserIdAndStatusInAndStartTimeAfterOrderByStartTimeAsc(
            Long userId, Collection<BookingStatus> statuses, LocalDateTime time, Pageable pageable);

    /** Lịch đang chờ duyệt, cũ nhất lên trước. */
    @EntityGraph(attributePaths = {"room", "user"})
    List<Booking> findByStatusOrderByCreatedAtAsc(BookingStatus status, Pageable pageable);

    /** Lịch đã duyệt trong khoảng [from, to), dùng tính số giờ sử dụng phòng. */
    @Query("""
            select b from Booking b
            join fetch b.room
            where b.status = com.example.roombooking.entity.Booking.BookingStatus.APPROVED
              and b.startTime >= :from
              and b.startTime < :to
            """)
    List<Booking> findApprovedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // ===================== Thống kê =====================

    long countByStatus(BookingStatus status);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, BookingStatus status);

    long countByStartTimeGreaterThanEqualAndStartTimeLessThan(LocalDateTime from, LocalDateTime toExclusive);

    /** Số lịch theo trạng thái trong khoảng [from, to). Mỗi dòng: {BookingStatus, Long}. */
    @Query("""
            select b.status, count(b) from Booking b
            where b.startTime >= :from and b.startTime < :to
            group by b.status
            """)
    List<Object[]> countGroupedByStatus(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Thời điểm bắt đầu của mọi lịch trong khoảng, để service gom theo tháng bằng
     * DateTimeUtils.monthKey (cách này chạy trên mọi loại DB, không phụ thuộc hàm của MySQL).
     */
    @Query("select b.startTime from Booking b where b.startTime >= :from and b.startTime < :to")
    List<LocalDateTime> findStartTimesBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Mỗi dòng: {roomId, tổng số lịch}. Xếp phòng được đặt nhiều nhất lên đầu. */
    @Query("""
            select b.room.id, count(b) from Booking b
            where b.startTime >= :from and b.startTime < :to
            group by b.room.id
            order by count(b) desc
            """)
    List<Object[]> countGroupedByRoom(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                      Pageable pageable);
}