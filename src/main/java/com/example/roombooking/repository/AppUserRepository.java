package com.example.roombooking.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.roombooking.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {

    Optional<AppUser> findByUsernameIgnoreCase(String username);

    Optional<AppUser> findByEmailIgnoreCase(String email);

    /** Đăng nhập bằng username HOẶC email (không phân biệt hoa thường). */
    @Query("""
            select u from AppUser u
            where lower(u.username) = lower(:login)
               or lower(u.email) = lower(:login)
            """)
    Optional<AppUser> findByUsernameOrEmail(@Param("login") String login);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countByEnabledTrue();

    long countByRoles_Name(String roleName);

    /** Người dùng đang hoạt động có vai trò cho trước (vd: gửi thông báo tới toàn bộ quản trị viên). */
    List<AppUser> findByRoles_NameAndEnabledTrue(String roleName);

    @Modifying(clearAutomatically = true)
    @Query("update AppUser u set u.lastLoginAt = :time where u.id = :id")
    int updateLastLoginAt(@Param("id") Long id, @Param("time") LocalDateTime time);

    @Modifying(clearAutomatically = true)
    @Query("update AppUser u set u.password = :password where u.id = :id")
    int updatePassword(@Param("id") Long id, @Param("password") String encodedPassword);
}