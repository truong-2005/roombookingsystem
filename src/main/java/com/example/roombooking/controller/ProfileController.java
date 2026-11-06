package com.example.roombooking.controller;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.dto.profile.ChangePasswordRequest;
import com.example.roombooking.dto.profile.ChangePasswordResponse;
import com.example.roombooking.dto.profile.ProfileResponse;
import com.example.roombooking.dto.profile.UpdateProfileRequest;
import com.example.roombooking.dto.profile.UpdateProfileResponse;
import com.example.roombooking.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.PROFILE)
@Tag(name = "Profile", description = "Hồ sơ và mật khẩu của tài khoản đang đăng nhập")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Xem hồ sơ của tôi")
    public ProfileResponse getProfile() {
        return profileService.getProfile();
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cập nhật hồ sơ", description = "Gửi multipart/form-data; ảnh có thể là avatarUrl hoặc avatarFile, hoặc bật removeAvatar.")
    public UpdateProfileResponse updateProfile(@Valid @ModelAttribute UpdateProfileRequest request) {
        return profileService.updateProfile(request);
    }

    @PatchMapping("/password")
    @Operation(summary = "Đổi mật khẩu")
    public ChangePasswordResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return profileService.changePassword(request);
    }
}
