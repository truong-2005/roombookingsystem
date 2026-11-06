package com.example.roombooking.controller;

import java.beans.ConstructorProperties;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.roombooking.dto.auth.AuthUserResponse;
import com.example.roombooking.dto.auth.ForgotPasswordRequest;
import com.example.roombooking.dto.auth.ForgotPasswordResponse;
import com.example.roombooking.dto.auth.LogoutRequest;
import com.example.roombooking.dto.auth.LogoutResponse;
import com.example.roombooking.dto.common.ErrorResponse;
import com.example.roombooking.dto.common.ValidationErrorResponse;
import com.example.roombooking.dto.auth.LoginRequest;
import com.example.roombooking.dto.auth.LoginResponse;
import com.example.roombooking.dto.auth.RefreshTokenRequest;
import com.example.roombooking.dto.auth.RefreshTokenResponse;
import com.example.roombooking.dto.auth.RegisterRequest;
import com.example.roombooking.dto.auth.ResetPasswordRequest;
import com.example.roombooking.dto.auth.ResetPasswordResponse;
import com.example.roombooking.dto.auth.VerifyResetTokenRequest;
import com.example.roombooking.dto.captcha.CaptchaChallengeResponse;
import com.example.roombooking.security.RateLimiter;
import com.example.roombooking.service.AuthService;
import com.example.roombooking.service.CaptchaService;
import com.example.roombooking.service.PasswordResetService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Đăng ký, đăng nhập và làm mới token")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final RateLimiter rateLimiter;
    private final PasswordResetService passwordResetService;

    @ConstructorProperties({"authService", "captchaService", "rateLimiter", "passwordResetService"})
    public AuthController(AuthService authService, CaptchaService captchaService, RateLimiter rateLimiter,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.rateLimiter = rateLimiter;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/captcha")
    @Operation(summary = "Lấy CAPTCHA", description = "Tạo câu hỏi CAPTCHA dùng một lần cho đăng nhập và đăng ký.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo CAPTCHA thành công",
                    content = @Content(schema = @Schema(implementation = CaptchaChallengeResponse.class))),
            @ApiResponse(responseCode = "429", description = "Gọi quá giới hạn; xem header Retry-After",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CaptchaChallengeResponse> getCaptcha(HttpServletRequest request) {
        rateLimiter.checkCaptchaChallengeLimit(clientIp(request));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(captchaService.createChallenge());
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập", description = "Đăng nhập bằng tên tài khoản hoặc email và CAPTCHA; trả access token cùng refresh token.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Thông tin không hợp lệ hoặc CAPTCHA sai",
                    content = @Content(schema = @Schema(oneOf = {ErrorResponse.class, ValidationErrorResponse.class}))),
            @ApiResponse(responseCode = "401", description = "Tên đăng nhập hoặc mật khẩu không đúng",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Tài khoản bị khóa hoặc vô hiệu hóa",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Đăng nhập quá giới hạn; xem header Retry-After",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        rateLimiter.checkLoginLimit(ip);
        LoginResponse response = authService.login(request, ip, httpRequest.getHeader("User-Agent"));
        rateLimiter.resetLoginLimit(ip);
        return response;
    }

    @PostMapping("/admin/login")
    @Operation(summary = "Đăng nhập quản trị viên", description = "Chỉ chấp nhận tài khoản có vai trò ADMIN; yêu cầu CAPTCHA.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Thông tin không hợp lệ hoặc CAPTCHA sai",
                    content = @Content(schema = @Schema(oneOf = {ErrorResponse.class, ValidationErrorResponse.class}))),
            @ApiResponse(responseCode = "401", description = "Tên đăng nhập hoặc mật khẩu không đúng",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Tài khoản không có quyền quản trị hoặc đã bị khóa",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Đăng nhập quá giới hạn; xem header Retry-After",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public LoginResponse adminLogin(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        rateLimiter.checkLoginLimit(ip);
        LoginResponse response = authService.adminLogin(request, ip, httpRequest.getHeader("User-Agent"));
        rateLimiter.resetLoginLimit(ip);
        return response;
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Làm mới token", description = "Đổi refresh token hiện tại lấy access token và refresh token mới. Refresh token cũ bị thu hồi.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Làm mới token thành công",
                    content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Refresh token không hợp lệ hoặc đã hết hạn",
                    content = @Content(schema = @Schema(oneOf = {ErrorResponse.class, ValidationErrorResponse.class})))
    })
    public RefreshTokenResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request,
                                             HttpServletRequest httpRequest) {
        return authService.refreshToken(request, clientIp(httpRequest), httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản người dùng",
            description = "Tạo tài khoản và tự động gán ROLE_USER. Role được xác định bởi endpoint; không nhận role từ JSON.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo tài khoản thành công, trả về token đăng nhập",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Thông tin không hợp lệ hoặc CAPTCHA sai",
                    content = @Content(schema = @Schema(oneOf = {ErrorResponse.class, ValidationErrorResponse.class}))),
            @ApiResponse(responseCode = "409", description = "Tên tài khoản hoặc email đã được sử dụng",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Đăng ký quá giới hạn; xem header Retry-After",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public LoginResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        rateLimiter.checkRegisterLimit(ip);
        return authService.register(request, ip, httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/admin/register")
    @Operation(summary = "Đăng ký tài khoản quản trị viên",
            description = "Tạo tài khoản với ROLE_USER và ROLE_ADMIN. Chỉ tạo admin đầu tiên khi hệ thống chưa có admin; "
                    + "các lần tạo tiếp theo cần access token của admin hiện tại qua nút Authorize. Role được xác định bởi endpoint, "
                    + "không nhận role từ JSON.")
    @SecurityRequirements({
            @SecurityRequirement(name = com.example.roombooking.config.OpenApiConfig.BEARER_SCHEME),
            @SecurityRequirement
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo tài khoản quản trị thành công, trả về token đăng nhập",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Thông tin không hợp lệ hoặc CAPTCHA sai",
                    content = @Content(schema = @Schema(oneOf = {ErrorResponse.class, ValidationErrorResponse.class}))),
            @ApiResponse(responseCode = "403", description = "Đã có quản trị viên; yêu cầu đăng nhập bằng tài khoản ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Tên tài khoản hoặc email đã được sử dụng",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Đăng ký quá giới hạn; xem header Retry-After",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public LoginResponse adminRegister(@Valid @RequestBody RegisterRequest request,
                                       HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        rateLimiter.checkRegisterLimit(ip);
        return authService.adminRegister(request, ip, httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất", description = "Thu hồi refresh token của phiên được gửi trong request body.")
    public LogoutResponse logout(@Valid @RequestBody LogoutRequest request) {
        return authService.logout(request);
    }

    @PostMapping("/admin/logout")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Đăng xuất quản trị viên")
    public LogoutResponse adminLogout(@Valid @RequestBody LogoutRequest request) {
        return authService.adminLogout(request);
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Đăng xuất khỏi mọi thiết bị", description = "Thu hồi toàn bộ refresh token của tài khoản đang đăng nhập.")
    public LogoutResponse logoutAllDevices() {
        return authService.logoutAllDevices();
    }

    @GetMapping("/me")
    @Operation(summary = "Thông tin tài khoản hiện tại")
    public AuthUserResponse me() {
        return authService.me();
    }

    @PostMapping("/forgot-password")
    @Operation(operationId = "forgotPassword", summary = "Quên mật khẩu",
            description = "Gửi email hướng dẫn đặt lại mật khẩu nếu địa chỉ email được đăng ký trong hệ thống.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã tiếp nhận yêu cầu",
                    content = @Content(schema = @Schema(implementation = ForgotPasswordResponse.class))),
            @ApiResponse(responseCode = "400", description = "Email không hợp lệ",
                    content = @Content(schema = @Schema(oneOf = {ErrorResponse.class, ValidationErrorResponse.class})))
    })
    public ForgotPasswordResponse forgotPassword(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true, description = "Địa chỉ email dùng để đăng ký tài khoản.",
                    content = @Content(schema = @Schema(implementation = ForgotPasswordRequest.class)))
            ForgotPasswordRequest request) {
        return passwordResetService.forgotPassword(request);
    }

    @PostMapping("/verify-reset-token")
    @Operation(operationId = "verifyPasswordResetToken", summary = "Kiểm tra mã đặt lại mật khẩu",
            description = "Xác nhận mã còn hiệu lực trước khi cho phép người dùng đặt mật khẩu mới.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về trạng thái hợp lệ của mã",
                    content = @Content(schema = @Schema(implementation = ResetPasswordResponse.class))),
            @ApiResponse(responseCode = "400", description = "Mã không hợp lệ hoặc đã hết hạn",
                    content = @Content(schema = @Schema(oneOf = {ErrorResponse.class, ValidationErrorResponse.class})))
    })
    public ResetPasswordResponse verifyResetToken(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true, description = "Mã đặt lại mật khẩu nhận được qua email.",
                    content = @Content(schema = @Schema(implementation = VerifyResetTokenRequest.class)))
            VerifyResetTokenRequest request) {
        return passwordResetService.verifyToken(request);
    }

    @PostMapping("/reset-password")
    @Operation(operationId = "resetPassword", summary = "Đặt lại mật khẩu",
            description = "Đổi mật khẩu bằng mã đặt lại còn hiệu lực; mật khẩu xác nhận phải khớp.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đặt lại mật khẩu thành công",
                    content = @Content(schema = @Schema(implementation = ResetPasswordResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc mã đã hết hạn",
                    content = @Content(schema = @Schema(oneOf = {ErrorResponse.class, ValidationErrorResponse.class})))
    })
    public ResetPasswordResponse resetPassword(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true, description = "Mã hợp lệ, mật khẩu mới và giá trị xác nhận.",
                    content = @Content(schema = @Schema(implementation = ResetPasswordRequest.class)))
            ResetPasswordRequest request) {
        return passwordResetService.resetPassword(request);
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return ip != null && !ip.isBlank() ? ip : "unknown";
    }
}
