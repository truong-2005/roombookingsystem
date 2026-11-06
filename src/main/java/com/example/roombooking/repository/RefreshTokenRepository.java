package com.example.roombooking.repository;

import com.example.roombooking.entity.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByToken(String token);

    /** Thu hồi mọi phiên của một người (đổi mật khẩu, khóa tài khoản, đăng xuất tất cả). */
    @Modifying(clearAutomatically = true)
    @Query("update RefreshToken t set t.revoked = true where t.user.id = :userId and t.revoked = false")
    int revokeAllByUserId(@Param("userId") Long userId);

    /** Thu hồi mọi phiên của một người, trừ phiên hiện tại. */
    @Modifying(clearAutomatically = true)
    @Query("""
            update RefreshToken t set t.revoked = true
            where t.user.id = :userId and t.revoked = false and t.token <> :keepToken
            """)
    int revokeAllByUserIdExcept(@Param("userId") Long userId, @Param("keepToken") String keepToken);

    long countByUserIdAndRevokedFalseAndExpiryDateAfter(Long userId, LocalDateTime now);

    /** Dọn token hết hạn hoặc đã thu hồi (gọi từ một job định kỳ). */
    @Modifying
    @Query("delete from RefreshToken t where t.expiryDate < :now or t.revoked = true")
    int deleteExpiredOrRevoked(@Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from RefreshToken t where t.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}