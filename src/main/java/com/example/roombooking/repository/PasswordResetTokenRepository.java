package com.example.roombooking.repository;

import com.example.roombooking.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<PasswordResetToken> findByToken(String token);

    /** Vô hiệu các mã cũ chưa dùng khi người dùng yêu cầu mã mới, hoặc sau khi đặt lại thành công. */
    @Modifying(clearAutomatically = true)
    @Query("update PasswordResetToken t set t.used = true where t.user.id = :userId and t.used = false")
    int invalidateAllByUserId(@Param("userId") Long userId);

    /** Giới hạn tần suất: số lần yêu cầu từ một thời điểm, chống spam gửi email. */
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);

    @Modifying
    @Query("delete from PasswordResetToken t where t.expiryDate < :now or t.used = true")
    int deleteExpiredOrUsed(@Param("now") LocalDateTime now);

    /** Xóa toàn bộ mã đặt lại mật khẩu của một người dùng (trước khi xóa tài khoản). */
    @Modifying
    @Query("delete from PasswordResetToken t where t.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}