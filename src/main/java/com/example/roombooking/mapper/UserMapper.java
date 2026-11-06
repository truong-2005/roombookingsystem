package com.example.roombooking.mapper;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.dto.auth.AuthUserResponse;
import com.example.roombooking.dto.auth.RegisterRequest;
import com.example.roombooking.dto.auth.RegisterResponse;
import com.example.roombooking.dto.profile.ProfileResponse;
import com.example.roombooking.dto.profile.UpdateProfileRequest;
import com.example.roombooking.dto.user.UserCreateRequest;
import com.example.roombooking.dto.user.UserDetailResponse;
import com.example.roombooking.dto.user.UserResponse;
import com.example.roombooking.dto.user.UserUpdateRequest;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.AppUser.AvatarSource;


@Component
public class UserMapper {

    // ===================== Entity -> Response =====================

    public AuthUserResponse toAuthUserResponse(AppUser u) {
        if (u == null) return null;
        return new AuthUserResponse(
                u.getId(), u.getUsername(), u.getEmail(), u.getFullName(),
                u.getAvatarSource(), resolveAvatarUrl(u), roleNames(u), u.isEnabled());
    }

    public RegisterResponse toRegisterResponse(AppUser u) {
        if (u == null) return null;
        return new RegisterResponse(
                u.getId(), u.getUsername(), u.getEmail(), u.getFullName(),
                resolveAvatarUrl(u), u.getCreatedAt());
    }

    public ProfileResponse toProfileResponse(AppUser u) {
        if (u == null) return null;
        return new ProfileResponse(
                u.getId(), u.getUsername(), u.getEmail(), u.getFullName(),
                u.getPhone(), u.getDepartment(),
                u.getAvatarSource(), resolveAvatarUrl(u), roleNames(u),
                u.getLastLoginAt(), u.getCreatedAt());
    }

    public UserResponse toUserResponse(AppUser u) {
        if (u == null) return null;
        return new UserResponse(
                u.getId(), u.getUsername(), u.getEmail(), u.getFullName(),
                u.getPhone(), u.getDepartment(),
                u.getAvatarSource(), resolveAvatarUrl(u), roleNames(u), u.isEnabled());
    }

    /** totalBookings do service đếm bằng BookingRepository rồi truyền vào. */
    public UserDetailResponse toUserDetailResponse(AppUser u, long totalBookings) {
        if (u == null) return null;
        return new UserDetailResponse(
                u.getId(), u.getUsername(), u.getEmail(), u.getFullName(),
                u.getPhone(), u.getDepartment(),
                u.getAvatarSource(), resolveAvatarUrl(u), roleNames(u), u.isEnabled(),
                totalBookings, u.getLastLoginAt(), u.getCreatedAt(), u.getUpdatedAt());
    }

    // ===================== Request -> Entity =====================

    /**
     * Đăng ký: chưa có mật khẩu và role, service tự gán (mật khẩu đã mã hóa, ROLE_USER).
     * Chỉ nhận ảnh dạng link; trang đăng ký không có upload file.
     */
    public AppUser toEntity(RegisterRequest req) {
        AppUser user = AppUser.builder()
                .username(req.username().trim())
                .email(req.email().trim().toLowerCase())
                .fullName(req.fullName().trim())
                .phone(blankToNull(req.phone()))
                .department(blankToNull(req.department()))
                .enabled(true)
                .build();
        applyAvatarLink(user, req.avatarUrl());
        return user;
    }

    /**
     * ADMIN tạo người dùng: chưa có mật khẩu, role và ảnh file.
     * Nếu có link ảnh thì gán ngay; nếu có avatarFile thì service lưu file rồi gọi
     * user.setAvatarSource(FILE), setAvatarPath(...), setAvatarContentType(...).
     */
    public AppUser toEntity(UserCreateRequest req) {
        AppUser user = AppUser.builder()
                .username(req.getUsername().trim())
                .email(req.getEmail().trim().toLowerCase())
                .fullName(req.getFullName().trim())
                .phone(blankToNull(req.getPhone()))
                .department(blankToNull(req.getDepartment()))
                .enabled(req.isEnabled())
                .build();
        applyAvatarLink(user, req.getAvatarUrl());
        return user;
    }

    // ===================== Cập nhật entity có sẵn =====================

    /** Không đụng tới mật khẩu, role, ảnh: các phần này service xử lý riêng. */
    public void updateEntity(AppUser user, UserUpdateRequest req) {
        user.setUsername(req.getUsername().trim());
        user.setEmail(req.getEmail().trim().toLowerCase());
        user.setFullName(req.getFullName().trim());
        user.setPhone(blankToNull(req.getPhone()));
        user.setDepartment(blankToNull(req.getDepartment()));
    }

    /** Hồ sơ cá nhân: không cho đổi username, mật khẩu, role, ảnh (service xử lý ảnh). */
    public void updateEntity(AppUser user, UpdateProfileRequest req) {
        user.setFullName(req.getFullName().trim());
        user.setEmail(req.getEmail().trim().toLowerCase());
        user.setPhone(blankToNull(req.getPhone()));
        user.setDepartment(blankToNull(req.getDepartment()));
    }

    // ===================== Helper =====================

    /** Link ngoài thì giữ nguyên, file đã lưu thì quy đổi thành URL tải về công khai. */
    public String resolveAvatarUrl(AppUser u) {
        if (u == null || u.getAvatarSource() == null) return null;
        return switch (u.getAvatarSource()) {
            case LINK -> u.getAvatarUrl();
            case FILE -> u.getAvatarPath() == null ? null : ApiConstants.FILES + "/public/" + u.getAvatarPath();
            case NONE -> null;
        };
    }

    @SuppressWarnings("java:S1612") // Keep lambda for null safety instead of method reference
    private Set<String> roleNames(AppUser u) {
        if (u.getRoles() == null) return Set.of();
        // TreeSet để thứ tự role ổn định giữa các lần gọi
        return u.getRoles().stream()
                .filter(Objects::nonNull)
                .map(role -> role.getName())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private void applyAvatarLink(AppUser user, String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            user.setAvatarSource(AvatarSource.LINK);
            user.setAvatarUrl(avatarUrl.trim());
        }
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}