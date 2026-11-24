package com.example.roombooking.service;

import com.example.roombooking.constant.SecurityConstants;
import com.example.roombooking.dto.auth.AuthUserResponse;
import com.example.roombooking.dto.auth.LoginRequest;
import com.example.roombooking.dto.auth.LoginResponse;
import com.example.roombooking.dto.auth.RegisterRequest;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.Role;
import com.example.roombooking.exception.AccountDisabledException;
import com.example.roombooking.exception.DuplicateResourceException;
import com.example.roombooking.exception.ForbiddenOperationException;
import com.example.roombooking.mapper.UserMapper;
import com.example.roombooking.repository.AppUserRepository;
import com.example.roombooking.repository.RoleRepository;
import com.example.roombooking.security.JwtService;
import com.example.roombooking.service.RefreshTokenService.IssuedToken;
import com.example.roombooking.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    @SuppressWarnings("unused")
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CaptchaService captchaService;

    @InjectMocks
    private AuthServiceImpl authService;

    private AppUser activeUser;

    @BeforeEach
    public void setUp() {
        Role userRole = new Role(1L, Role.USER, "Người dùng");
        activeUser = new AppUser();
        activeUser.setId(10L);
        activeUser.setUsername("user1");
        activeUser.setEmail("user1@example.com");
        activeUser.setPassword("encodedPassword123");
        activeUser.setEnabled(true);
        activeUser.setRoles(Set.of(userRole));
    }

    // ==========================================
    // Login Tests
    // ==========================================

    @Test
    @DisplayName("login ném BadCredentialsException khi không tìm thấy tài khoản")
    void login_WhenUserNotFound_ThrowsBadCredentialsException() {
        LoginRequest req = new LoginRequest("unknown", "password", "capId", "capAns");

        when(userRepository.findByUsernameOrEmail("unknown")).thenReturn(Optional.empty());

        BadCredentialsException ex1 = assertThrows(BadCredentialsException.class,
                () -> authService.login(req, "127.0.0.1", "JUnit"));
        assertNotNull(ex1);
    }

    @Test
    @DisplayName("login ném BadCredentialsException khi sai mật khẩu")
    void login_WhenPasswordIncorrect_ThrowsBadCredentialsException() {
        LoginRequest req = new LoginRequest("user1", "wrongpass", "capId", "capAns");

        when(userRepository.findByUsernameOrEmail("user1")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrongpass", activeUser.getPassword())).thenReturn(false);

        BadCredentialsException ex2 = assertThrows(BadCredentialsException.class,
                () -> authService.login(req, "127.0.0.1", "JUnit"));
        assertNotNull(ex2);
    }

    @Test
    @DisplayName("login ném AccountDisabledException khi tài khoản bị khóa")
    void login_WhenAccountDisabled_ThrowsAccountDisabledException() {
        activeUser.setEnabled(false);
        LoginRequest req = new LoginRequest("user1", "password123", "capId", "capAns");

        when(userRepository.findByUsernameOrEmail("user1")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password123", activeUser.getPassword())).thenReturn(true);

        AccountDisabledException ex3 = assertThrows(AccountDisabledException.class,
                () -> authService.login(req, "127.0.0.1", "JUnit"));
        assertNotNull(ex3);
    }

    @Test
    @DisplayName("adminLogin ném ForbiddenOperationException khi tài khoản không có quyền ADMIN")
    void adminLogin_WhenNotAdmin_ThrowsForbiddenOperationException() {
        LoginRequest req = new LoginRequest("user1", "password123", "capId", "capAns");

        when(userRepository.findByUsernameOrEmail("user1")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password123", activeUser.getPassword())).thenReturn(true);

        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class,
                () -> authService.adminLogin(req, "127.0.0.1", "JUnit"));
        assertTrue(ex.getMessage().contains("không có quyền quản trị"));
    }

    @Test
    @DisplayName("login thành công trả về LoginResponse với Access Token và Refresh Token")
    void login_WhenValid_ReturnsLoginResponse() {
        LoginRequest req = new LoginRequest("user1", "password123", "capId", "capAns");
        IssuedToken issuedToken = new IssuedToken("raw-refresh-token-xyz", null);
        AuthUserResponse authUser = new AuthUserResponse(
                10L, "user1", "user1@example.com", "User One",
                AppUser.AvatarSource.NONE, null, Set.of("ROLE_USER"), true
        );

        when(userRepository.findByUsernameOrEmail("user1")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password123", activeUser.getPassword())).thenReturn(true);
        when(refreshTokenService.issue(eq(activeUser), any(), any())).thenReturn(issuedToken);
        when(jwtService.generateAccessToken(activeUser)).thenReturn("mock.jwt.token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(userMapper.toAuthUserResponse(activeUser)).thenReturn(authUser);

        LoginResponse response = authService.login(req, "127.0.0.1", "JUnit");

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.accessToken());
        assertEquals("raw-refresh-token-xyz", response.refreshToken());
        assertEquals(SecurityConstants.TOKEN_TYPE, response.tokenType());
        assertEquals(900L, response.expiresIn());
        assertNotNull(response.user());
    }

    // ==========================================
    // Register Tests
    // ==========================================

    @Test
    @DisplayName("register ném DuplicateResourceException khi tên đăng nhập đã được dùng")
    void register_WhenUsernameExists_ThrowsDuplicateResourceException() {
        RegisterRequest req = new RegisterRequest(
                "user1", "new@example.com", "Password123@", "Password123@",
                "User Name", null, null, null, "capId", "capAns"
        );

        when(userRepository.existsByUsernameIgnoreCase("user1")).thenReturn(true);

        DuplicateResourceException ex4 = assertThrows(DuplicateResourceException.class,
                () -> authService.register(req, "127.0.0.1", "JUnit"));
        assertNotNull(ex4);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register ném DuplicateResourceException khi email đã được dùng")
    void register_WhenEmailExists_ThrowsDuplicateResourceException() {
        RegisterRequest req = new RegisterRequest(
                "newuser", "user1@example.com", "Password123@", "Password123@",
                "User Name", null, null, null, "capId", "capAns"
        );

        when(userRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("user1@example.com")).thenReturn(true);

        DuplicateResourceException ex5 = assertThrows(DuplicateResourceException.class,
                () -> authService.register(req, "127.0.0.1", "JUnit"));
        assertNotNull(ex5);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("adminRegister cho phép tạo Admin đầu tiên khi hệ thống chưa có Admin nào")
    void adminRegister_WhenFirstAdmin_AllowsCreation() {
        RegisterRequest req = new RegisterRequest(
                "admin", "admin@example.com", "Password123@", "Password123@",
                "Admin First", null, null, null, "capId", "capAns"
        );
        Role userRole = new Role(1L, Role.USER, "Người dùng");
        Role adminRole = new Role(2L, Role.ADMIN, "Quản trị viên");

        AppUser newAdmin = new AppUser();
        newAdmin.setUsername("admin");
        newAdmin.setEmail("admin@example.com");

        AppUser savedAdmin = new AppUser();
        savedAdmin.setId(1L);
        savedAdmin.setUsername("admin");
        savedAdmin.setEmail("admin@example.com");

        IssuedToken issuedToken = new IssuedToken("admin-refresh-token", null);
        AuthUserResponse authUser = new AuthUserResponse(
                1L, "admin", "admin@example.com", "Admin First",
                AppUser.AvatarSource.NONE, null, Set.of("ROLE_ADMIN", "ROLE_USER"), true
        );

        // Chưa có admin nào trong database
        when(userRepository.countByRoles_Name(Role.ADMIN)).thenReturn(0L);
        when(userRepository.existsByUsernameIgnoreCase("admin")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
        when(userMapper.toEntity(req)).thenReturn(newAdmin);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(roleRepository.findByName(Role.USER)).thenReturn(Optional.of(userRole));
        when(roleRepository.findByName(Role.ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(newAdmin)).thenReturn(savedAdmin);
        when(refreshTokenService.issue(eq(savedAdmin), any(), any())).thenReturn(issuedToken);
        when(jwtService.generateAccessToken(savedAdmin)).thenReturn("admin.jwt.token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(userMapper.toAuthUserResponse(savedAdmin)).thenReturn(authUser);

        LoginResponse response = authService.adminRegister(req, "127.0.0.1", "JUnit");

        assertNotNull(response);
        assertEquals("admin.jwt.token", response.accessToken());
        verify(userRepository, times(1)).save(newAdmin);
    }
}
