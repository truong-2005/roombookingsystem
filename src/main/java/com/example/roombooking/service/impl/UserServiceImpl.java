package com.example.roombooking.service.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.dto.common.PageResponse;
import com.example.roombooking.dto.profile.ChangePasswordResponse;
import com.example.roombooking.dto.user.AdminChangeUserPasswordRequest;
import com.example.roombooking.dto.user.UserCreateRequest;
import com.example.roombooking.dto.user.UserDetailResponse;
import com.example.roombooking.dto.user.UserResponse;
import com.example.roombooking.dto.user.UserRoleAssignmentRequest;
import com.example.roombooking.dto.user.UserStatusRequest;
import com.example.roombooking.dto.user.UserUpdateRequest;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.AppUser.AvatarSource;
import com.example.roombooking.entity.Role;
import com.example.roombooking.exception.AccountDisabledException;
import com.example.roombooking.exception.BadRequestException;
import com.example.roombooking.exception.DuplicateResourceException;
import com.example.roombooking.exception.ForbiddenOperationException;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.exception.UnauthorizedException;
import com.example.roombooking.mapper.UserMapper;
import com.example.roombooking.repository.AppUserRepository;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.NotificationRepository;
import com.example.roombooking.repository.PasswordResetTokenRepository;
import com.example.roombooking.repository.RefreshTokenRepository;
import com.example.roombooking.repository.RoleRepository;
import com.example.roombooking.service.RefreshTokenService;
import com.example.roombooking.service.UserService;
import com.example.roombooking.specification.UserSpecification;
import com.example.roombooking.util.FileStorageUtils;
import com.example.roombooking.util.FileStorageUtils.StoredFile;
import com.example.roombooking.util.DateTimeUtils;
import com.example.roombooking.util.PaginationUtils;


@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BookingRepository bookingRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final FileStorageUtils fileStorage;

    @java.beans.ConstructorProperties({"userRepository", "roleRepository", "bookingRepository", "notificationRepository", "passwordResetTokenRepository", "refreshTokenRepository", "refreshTokenService", "passwordEncoder", "userMapper", "fileStorage"})
    public UserServiceImpl(
        AppUserRepository userRepository,
        RoleRepository roleRepository,
        BookingRepository bookingRepository,
        NotificationRepository notificationRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        RefreshTokenRepository refreshTokenRepository,
        RefreshTokenService refreshTokenService,
        PasswordEncoder passwordEncoder,
        UserMapper userMapper,
        FileStorageUtils fileStorage
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.bookingRepository = bookingRepository;
        this.notificationRepository = notificationRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.fileStorage = fileStorage;
    }

    // ===================== Xem =====================

    @Override
    public PageResponse<UserResponse> search(String keyword, Boolean enabled, String role,
                                             int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PaginationUtils.build(page, size, sortBy, sortDir,
                ApiConstants.USER_SORT_FIELDS, "createdAt");
        Page<AppUser> result = userRepository.findAll(UserSpecification.search(keyword, enabled, role), pageable);
        return PaginationUtils.toPageResponse(result, userMapper::toUserResponse);
    }

    @Override
    public UserDetailResponse getById(Long id) {
        AppUser user = getEntity(id);
        return userMapper.toUserDetailResponse(user, bookingRepository.countByUserId(id));
    }

    // ===================== Thêm =====================

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest req) {
        String username = req.getUsername().trim();
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByUsernameIgnoreCase(username))
            throw new DuplicateResourceException("Người dùng", "tên đăng nhập", username);
        if (userRepository.existsByEmailIgnoreCase(email))
            throw new DuplicateResourceException("Người dùng", "email", email);

        AppUser user = userMapper.toEntity(req);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRoles(resolveRoles(req.getRoles()));
        applyAvatar(user, req.getAvatarUrl(), req.getAvatarFile(), false);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    // ===================== Sửa =====================

    @Override
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest req) {
        AppUser user = getEntity(id);
        String username = req.getUsername().trim();
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(username, id))
            throw new DuplicateResourceException("Người dùng", "tên đăng nhập", username);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id))
            throw new DuplicateResourceException("Người dùng", "email", email);

        boolean usernameChanged = !user.getUsername().equals(username);
        boolean passwordChanged = req.getNewPassword() != null && !req.getNewPassword().isBlank();

        userMapper.updateEntity(user, req);
        if (passwordChanged) user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        applyAvatar(user, req.getAvatarUrl(), req.getAvatarFile(), req.isRemoveAvatar());

        AppUser saved = userRepository.saveAndFlush(user);
        // Đổi tên đăng nhập hoặc mật khẩu thì buộc đăng nhập lại
        if (usernameChanged || passwordChanged) refreshTokenService.revokeAll(id);
        return userMapper.toUserResponse(saved);
    }

    @Override
    @Transactional
    public ChangePasswordResponse changePasswordByAdmin(Long id, AdminChangeUserPasswordRequest req) {
        AppUser current = getCurrentUserEntity();
        if (!current.hasRole(Role.ADMIN)) {
            throw new ForbiddenOperationException("Chỉ quản trị viên mới được đặt lại mật khẩu người dùng");
        }
        if (current.getId().equals(id)) {
            throw new BadRequestException("Không thể đặt lại mật khẩu của chính mình tại đây; hãy dùng chức năng đổi mật khẩu cá nhân");
        }

        AppUser user = getEntity(id);
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.saveAndFlush(user);
        boolean sessionsRevoked = refreshTokenService.revokeAll(id) > 0;

        return new ChangePasswordResponse(DateTimeUtils.now(), sessionsRevoked,
                "Mật khẩu đã được quản trị viên đặt lại; người dùng cần đăng nhập lại khi access token hiện tại hết hạn");
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long id, UserStatusRequest req) {
        AppUser user = getEntity(id);
        boolean enable = Boolean.TRUE.equals(req.enabled());
        if (!enable) {
            if (getCurrentUserEntity().getId().equals(id))
                throw new BadRequestException("Không thể tự khóa tài khoản của chính mình");
            assertNotLastAdmin(user, "khóa");
        }
        user.setEnabled(enable);
        AppUser saved = userRepository.saveAndFlush(user);
        if (!enable) refreshTokenService.revokeAll(id);
        return userMapper.toUserResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse assignRoles(Long id, UserRoleAssignmentRequest req) {
        AppUser user = getEntity(id);
        Set<Role> newRoles = resolveRoles(req.roles());
        boolean keepsAdmin = newRoles.stream().anyMatch(r -> Role.ADMIN.equals(r.getName()));

        if (user.hasRole(Role.ADMIN) && !keepsAdmin) {
            if (getCurrentUserEntity().getId().equals(id))
                throw new BadRequestException("Không thể tự gỡ quyền quản trị của chính mình");
            assertNotLastAdmin(user, "gỡ quyền quản trị của");
        }
        user.setRoles(newRoles);
        AppUser saved = userRepository.saveAndFlush(user);
        refreshTokenService.revokeAll(id); // quyền nằm trong token cũ, buộc cấp lại
        return userMapper.toUserResponse(saved);
    }

    // ===================== Xóa =====================

    @Override
    @Transactional
    public void delete(Long id) {
        if (getCurrentUserEntity().getId().equals(id))
            throw new BadRequestException("Không thể tự xóa tài khoản của chính mình");
        AppUser user = getEntity(id);
        assertNotLastAdmin(user, "xóa");
        if (bookingRepository.existsByUserId(id))
            throw new BadRequestException(
                    "Người dùng đã có lịch đặt phòng nên không thể xóa. Hãy khóa tài khoản thay vì xóa");

        String avatarPath = user.getAvatarSource() == AvatarSource.FILE ? user.getAvatarPath() : null;
        refreshTokenRepository.deleteByUserId(id);
        passwordResetTokenRepository.deleteByUserId(id);
        notificationRepository.deleteByUserId(id);
        userRepository.delete(user);
        userRepository.flush(); // lỗi khóa ngoại (nếu có) nổ ngay tại đây
        fileStorage.deleteAfterCommit(avatarPath);
    }

    // ===================== Dùng nội bộ =====================

    @Override
    public AppUser getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("người dùng", id));
    }

    @Override
    public AppUser getCurrentUserEntity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken)
            throw new UnauthorizedException();
        AppUser user = userRepository.findByUsernameOrEmail(auth.getName())
                .orElseThrow(UnauthorizedException::new);
        if (!user.isEnabled()) throw new AccountDisabledException();
        return user;
    }

    @Override
    public boolean applyAvatar(AppUser user, String avatarUrl, MultipartFile avatarFile, boolean removeAvatar) {
        boolean hasFile = avatarFile != null && !avatarFile.isEmpty();
        String url = avatarUrl != null ? avatarUrl.trim() : null;
        boolean hasUrl = url != null && !url.isEmpty();
        if (!hasFile && !hasUrl && !removeAvatar) return false;

        String oldPath = user.getAvatarSource() == AvatarSource.FILE ? user.getAvatarPath() : null;
        if (hasFile) {
            StoredFile f = fileStorage.saveImage(avatarFile, ApiConstants.UPLOAD_AVATAR_DIR);
            user.setAvatarSource(AvatarSource.FILE);
            user.setAvatarPath(f.path());
            user.setAvatarContentType(f.contentType());
            user.setAvatarUrl(null);
        } else if (hasUrl) {
            user.setAvatarSource(AvatarSource.LINK);
            user.setAvatarUrl(url);
            user.setAvatarPath(null);
            user.setAvatarContentType(null);
        } else {
            user.setAvatarSource(AvatarSource.NONE);
            user.setAvatarUrl(null);
            user.setAvatarPath(null);
            user.setAvatarContentType(null);
        }
        fileStorage.deleteAfterCommit(oldPath);
        return true;
    }

    // ===================== Helper =====================

    /** Bỏ trống thì mặc định ROLE_USER. Tên vai trò không tồn tại thì báo lỗi rõ ràng. */
    private Set<Role> resolveRoles(Set<String> names) {
        if (names == null || names.isEmpty()) {
            Role userRole = roleRepository.findByName(Role.USER)
                    .orElseThrow(() -> new ResourceNotFoundException("vai trò", "tên", Role.USER));
            return new HashSet<>(Set.of(userRole));
        }
        Set<String> wanted = names.stream().map(n -> n.trim()).collect(Collectors.toSet());
        Set<Role> found = new HashSet<>(roleRepository.findByNameIn(wanted));
        Set<String> foundNames = found.stream().map(r -> r.getName()).collect(Collectors.toSet());
        wanted.removeAll(foundNames);
        if (!wanted.isEmpty())
            throw new BadRequestException("Vai trò không tồn tại: " + String.join(", ", wanted));
        return found;
    }

    private void assertNotLastAdmin(AppUser user, String action) {
        if (user.hasRole(Role.ADMIN) && userRepository.countByRoles_Name(Role.ADMIN) <= 1)
            throw new BadRequestException("Không thể " + action + " quản trị viên cuối cùng của hệ thống");
    }
}
