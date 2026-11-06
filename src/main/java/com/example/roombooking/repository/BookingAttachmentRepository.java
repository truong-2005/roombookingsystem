package com.example.roombooking.repository;

import com.example.roombooking.entity.BookingAttachment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingAttachmentRepository extends JpaRepository<BookingAttachment, Long> {

    @EntityGraph(attributePaths = {"booking", "uploadedBy"})
    List<BookingAttachment> findByBookingIdOrderByUploadedAtAsc(Long bookingId);

    /** Tìm theo cả id file và id lịch đặt, tránh tải file của lịch khác bằng cách đoán id. */
    @EntityGraph(attributePaths = {"booking", "booking.user", "uploadedBy"})
    Optional<BookingAttachment> findByIdAndBookingId(Long id, Long bookingId);

    long countByBookingId(Long bookingId);

    /** Tổng dung lượng file của một lịch đặt (byte), 0 nếu chưa có file. */
    @Query("select coalesce(sum(a.fileSize), 0) from BookingAttachment a where a.booking.id = :bookingId")
    long sumFileSizeByBookingId(@Param("bookingId") Long bookingId);
}