package com.example.roombooking.service;

import org.springframework.web.multipart.MultipartFile;

import com.example.roombooking.dto.common.PageResponse;
import com.example.roombooking.dto.user.UserCreateRequest;
import com.example.roombooking.dto.user.AdminChangeUserPasswordRequest;
import com.example.roombooking.dto.user.UserDetailResponse;
import com.example.roombooking.dto.user.UserResponse;
import com.example.roombooking.dto.user.UserRoleAssignmentRequest;
import com.example.roombooking.dto.user.UserStatusRequest;
import com.example.roombooking.dto.user.UserUpdateRequest;
import com.example.roombooking.dto.profile.ChangePasswordResponse;
import com.example.roombooking.entity.AppUser;

public interface UserService {

    PageResponse<UserResponse> search(String keyword, Boolean enabled, String role,
                                      int page, int size, String sortBy, String sortDir);

    UserDetailResponse getById(Long id);

    UserResponse create(UserCreateRequest req);

    UserResponse update(Long id, UserUpdateRequest req);

    ChangePasswordResponse changePasswordByAdmin(Long id, AdminChangeUserPasswordRequest req);

    void delete(Long id);

    UserResponse updateStatus(Long id, UserStatusRequest req);

    UserResponse assignRoles(Long id, UserRoleAssignmentRequest req);

    // ===== Dùng nội bộ giữa các service =====

    AppUser getEntity(Long id);

    /** Người đang đăng nhập (ném UnauthorizedException nếu chưa đăng nhập). */
    AppUser getCurrentUserEntity();

    /** Áp ảnh đại diện (link / file / xóa) lên entity. Trả true nếu có thay đổi. */
    boolean applyAvatar(AppUser user, String avatarUrl, MultipartFile avatarFile, boolean removeAvatar);
}
