package com.example.roombooking.service;

import com.example.roombooking.dto.profile.ChangePasswordRequest;
import com.example.roombooking.dto.profile.ChangePasswordResponse;
import com.example.roombooking.dto.profile.ProfileResponse;
import com.example.roombooking.dto.profile.UpdateProfileRequest;
import com.example.roombooking.dto.profile.UpdateProfileResponse;

public interface ProfileService {

    /** Xem thông tin tài khoản đang đăng nhập. */
    ProfileResponse getProfile();

    /** Cập nhật thông tin, ảnh đại diện (link hoặc file, hoặc xóa ảnh). */
    UpdateProfileResponse updateProfile(UpdateProfileRequest req);

    ChangePasswordResponse changePassword(ChangePasswordRequest req);
}