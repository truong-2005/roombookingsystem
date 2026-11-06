package com.example.roombooking.service.impl;

import com.example.roombooking.constant.SecurityConstants;
import com.example.roombooking.dto.auth.*;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.Role;
import com.example.roombooking.exception.AccountDisabledException;
import com.example.roombooking.exception.DuplicateResourceException;
import com.example.roombooking.exception.ForbiddenOperationException;
import com.example.roombooking.mapper.UserMapper;
import com.example.roombooking.repository.AppUserRepository;
import com.example.roombooking.repository.RoleRepository;
import com.example.roombooking.security.JwtService;
import com.example.roombooking.service.AuthService;
import com.example.roombooking.service.CaptchaService;
import com.example.roombooking.service.RefreshTokenService;
import com.example.roombooking.service.RefreshTokenService.IssuedToken;
import com.example.roombooking.service.UserService;
import com.example.roombooking.util.DateTimeUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final UserMapper userMapper;
    private final CaptchaService captchaService;

    @java.beans.ConstructorProperties({"userRepository", "roleRepository", "passwordEncoder", "jwtService", "refreshTokenService", "userService", "userMapper", "captchaService"})
    public AuthServiceImpl(
        AppUserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        RefreshTokenService refreshTokenService,
        UserService userService,
        UserMapper userMapper,
        CaptchaService captchaService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
        this.userMapper = userMapper;
        this.captchaService = captchaService;
    }

    // ===================== Đăng nhập =====================

    @Override
    public LoginResponse login(LoginRequest req, String ipAddress, String userAgent) {
        return doLogin(req, false, ipAddress, userAgent);
    }

    @Override
    public LoginResponse adminLogin(LoginRequest req, String ipAddress, String userAgent) {
        return doLogin(req, true, ipAddress, userAgent);
    }

    private LoginResponse doLogin(LoginRequest req, boolean adminOnly, String ip, String ua) {
        captchaService.verify(req.captchaId(), req.captchaAnswer());

        // Đăng nhập bằng username HOẶC email; sai tài khoản và sai mật khẩu trả cùng một lỗi
        AppUser user = userRepository.findByUsernameOrEmail(req.usernameOrEmail().trim())
                .orElseThrow(() -> new BadCredentialsException("Bad credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPassword()))
            throw new BadCredentialsException("Bad credentials");
        if (!user.isEnabled()) throw new AccountDisabledException();
        if (adminOnly && !user.hasRole(Role.ADMIN))
            throw new ForbiddenOperationException("Tài khoản này không có quyền quản trị");

        user.setLastLoginAt(DateTimeUtils.now());
        IssuedToken refresh = refreshTokenService.issue(user, ip, ua);
        return new LoginResponse(
                jwtService.generateAccessToken(user),
                refresh.rawToken(),
                SecurityConstants.TOKEN_TYPE,
                jwtService.getAccessTokenExpirationSeconds(),
                userMapper.toAuthUserResponse(user));
    }

    // ===================== Đăng ký =====================

    @Override
    public LoginResponse register(RegisterRequest req, String ipAddress, String userAgent) {
        return createAccount(req, false, ipAddress, userAgent);
    }

    @Override
    public LoginResponse adminRegister(RegisterRequest req, String ipAddress, String userAgent) {
        // Chưa có admin nào: cho phép tạo admin đầu tiên. Từ admin thứ hai: chỉ ADMIN đang đăng nhập mới được tạo.
        if (userRepository.countByRoles_Name(Role.ADMIN) > 0 && !isCurrentUserAdmin())
            throw new ForbiddenOperationException("Chỉ quản trị viên mới được tạo tài khoản quản trị");
        return createAccount(req, true, ipAddress, userAgent);
    }

    private LoginResponse createAccount(RegisterRequest req, boolean admin, String ipAddress, String userAgent) {
        captchaService.verify(req.captchaId(), req.captchaAnswer());

        String username = req.username().trim();
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByUsernameIgnoreCase(username))
            throw new DuplicateResourceException("Tài khoản", "tên đăng nhập", username);
        if (userRepository.existsByEmailIgnoreCase(email))
            throw new DuplicateResourceException("Tài khoản", "email", email);

        AppUser user = userMapper.toEntity(req);
        user.setPassword(passwordEncoder.encode(req.password()));

        Set<Role> roles = new HashSet<>();
        roles.add(getOrCreateRole(Role.USER, "Người dùng"));
        if (admin) roles.add(getOrCreateRole(Role.ADMIN, "Quản trị viên"));
        user.setRoles(roles);

        user.setLastLoginAt(DateTimeUtils.now());
        AppUser savedUser = userRepository.save(user);
        IssuedToken refresh = refreshTokenService.issue(savedUser, ipAddress, userAgent);
        return new LoginResponse(
                jwtService.generateAccessToken(savedUser),
                refresh.rawToken(),
                SecurityConstants.TOKEN_TYPE,
                jwtService.getAccessTokenExpirationSeconds(),
                userMapper.toAuthUserResponse(savedUser));
    }

    // ===================== Refresh token / Đăng xuất =====================

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest req, String ipAddress, String userAgent) {
        IssuedToken issued = refreshTokenService.rotate(req.refreshToken(), ipAddress, userAgent);
        AppUser user = issued.entity().getUser();
        return new RefreshTokenResponse(
                jwtService.generateAccessToken(user),
                issued.rawToken(),
                SecurityConstants.TOKEN_TYPE,
                jwtService.getAccessTokenExpirationSeconds());
    }

    @Override
    public LogoutResponse logout(LogoutRequest req) {
        return doLogout(req.refreshToken(), false);
    }

    @Override
    public LogoutResponse adminLogout(LogoutRequest req) {
        return doLogout(req.refreshToken(), true);
    }

    /** Idempotent: token không tồn tại vẫn coi như đã đăng xuất. */
    private LogoutResponse doLogout(String rawToken, boolean adminOnly) {
        refreshTokenService.find(rawToken).ifPresent(t -> {
            if (adminOnly && !t.getUser().hasRole(Role.ADMIN))
                throw new ForbiddenOperationException("Phiên đăng nhập này không thuộc tài khoản quản trị");
            refreshTokenService.revoke(rawToken);
        });
        return new LogoutResponse(true, "Đăng xuất thành công");
    }

    @Override
    public LogoutResponse logoutAllDevices() {
        AppUser current = userService.getCurrentUserEntity();
        int n = refreshTokenService.revokeAll(current.getId());
        return new LogoutResponse(true, "Đã đăng xuất khỏi " + n + " thiết bị");
    }

    @Override
    @Transactional(readOnly = true)
    public AuthUserResponse me() {
        return userMapper.toAuthUserResponse(userService.getCurrentUserEntity());
    }

    // ===================== Helper =====================

    private Role getOrCreateRole(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder().name(name).description(description).build()));
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getAuthorities().stream().anyMatch(a -> Role.ADMIN.equals(a.getAuthority()));
    }
}
