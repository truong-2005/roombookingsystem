package com.example.roombooking.repository;

import com.example.roombooking.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    Optional<Room> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<Room> findByActiveTrueOrderByCodeAsc();

    long countByActive(boolean active);

    /**
     * Khóa ghi dòng của phòng (SELECT ... FOR UPDATE).
     * Gọi ở đầu transaction đặt / duyệt / dời lịch, trước khi kiểm tra trùng giờ,
     * để hai yêu cầu cùng phòng được xử lý lần lượt thay vì song song.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}