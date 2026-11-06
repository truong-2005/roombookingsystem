package com.example.roombooking.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.roombooking.dto.profile.ChangePasswordRequest;
import com.example.roombooking.dto.profile.ChangePasswordResponse;
import com.example.roombooking.dto.profile.ProfileResponse;
import com.example.roombooking.dto.profile.UpdateProfileRequest;
import com.example.roombooking.dto.profile.UpdateProfileResponse;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.exception.DuplicateResourceException;
import com.example.roombooking.exception.InvalidPasswordException;
import com.example.roombooking.mapper.UserMapper;
import com.example.roombooking.repository.AppUserRepository;
import com.example.roombooking.service.EmailService;
import com.example.roombooking.service.ProfileService;
import com.example.roombooking.service.RefreshTokenService;
import com.example.roombooking.service.UserService;
import com.example.roombooking.util.DateTimeUtils;


@Service
@Transactional(readOnly = true)
public class ProfileServiceImpl implements ProfileService {

    private final AppUserRepository userRepository;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @java.beans.ConstructorProperties({"userRepository", "userService", "refreshTokenService", "emailService", "passwordEncoder", "userMapper"})
    public ProfileServiceImpl(
        AppUserRepository userRepository,
        UserService userService,
        RefreshTokenService refreshTokenService,
        EmailService emailService,
        PasswordEncoder passwordEncoder,
        UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Override
    public ProfileResponse getProfile() {
        return userMapper.toProfileResponse(userService.getCurrentUserEntity());
    }

    @Override
    @Transactional
    public UpdateProfileResponse updateProfile(UpdateProfileRequest req) {
        AppUser user = userService.getCurrentUserEntity();
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId()))
            throw new DuplicateResourceException("Người dùng", "email", email);

        userMapper.updateEntity(user, req);
        boolean avatarChanged = userService.applyAvatar(
                user, req.getAvatarUrl(), req.getAvatarFile(), req.isRemoveAvatar());
        AppUser saved = userRepository.save(user);
        return new UpdateProfileResponse(userMapper.toProfileResponse(saved), avatarChanged,
                "Cập nhật thông tin thành công");
    }

    @Override
    @Transactional
    public ChangePasswordResponse changePassword(ChangePasswordRequest req) {
        AppUser user = userService.getCurrentUserEntity();
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword()))
            throw new InvalidPasswordException();

        Long userId = user.getId();
        String email = user.getEmail();
        String fullName = user.getFullName();

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.saveAndFlush(user);
        refreshTokenService.revokeAll(userId);
        emailService.sendPasswordChangedEmail(email, fullName);

        return new ChangePasswordResponse(DateTimeUtils.now(), true,
                "Đổi mật khẩu thành công. Vui lòng đăng nhập lại trên tất cả thiết bị");
    }
}