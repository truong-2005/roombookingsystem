package com.example.roombooking.controller;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.dto.common.PageResponse;
import com.example.roombooking.dto.user.UserCreateRequest;
import com.example.roombooking.dto.user.AdminChangeUserPasswordRequest;
import com.example.roombooking.dto.user.UserDetailResponse;
import com.example.roombooking.dto.user.UserResponse;
import com.example.roombooking.dto.user.UserRoleAssignmentRequest;
import com.example.roombooking.dto.user.UserStatusRequest;
import com.example.roombooking.dto.user.UserUpdateRequest;
import com.example.roombooking.dto.profile.ChangePasswordResponse;
import com.example.roombooking.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping(ApiConstants.USERS)
@Validated
@Tag(name = "Users", description = "Quản lý tài khoản người dùng (ADMIN)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm người dùng", description = "Tìm và phân trang người dùng theo từ khóa, trạng thái hoặc vai trò.")
    public PageResponse<UserResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return userService.search(keyword, enabled, role, page, size, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết người dùng")
    public UserDetailResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tạo người dùng", description = "Tạo tài khoản. Có thể gửi ảnh đại diện bằng avatarUrl hoặc avatarFile.")
    public UserResponse create(@Valid @ModelAttribute UserCreateRequest request) {
        return userService.create(request);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cập nhật người dùng", description = "Cập nhật thông tin tài khoản. Có thể gửi newPassword để đặt mật khẩu mới; dùng avatarUrl hoặc avatarFile để đổi ảnh.")
    public UserResponse update(@PathVariable Long id, @Valid @ModelAttribute UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Đặt lại mật khẩu người dùng", description = "ADMIN đặt mật khẩu mới cho tài khoản khác; các refresh token hiện tại sẽ bị thu hồi.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã đặt lại mật khẩu",
                    content = @Content(schema = @Schema(implementation = ChangePasswordResponse.class))),
            @ApiResponse(responseCode = "400", description = "Mật khẩu xác nhận không khớp hoặc ADMIN tự đặt lại mật khẩu của mình"),
            @ApiResponse(responseCode = "403", description = "Chỉ ADMIN được sử dụng endpoint này"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng")
    })
    public ChangePasswordResponse changePassword(@PathVariable Long id,
            @Valid @RequestBody AdminChangeUserPasswordRequest request) {
        return userService.changePasswordByAdmin(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Thay đổi trạng thái người dùng")
    public UserResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return userService.updateStatus(id, request);
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Gán vai trò cho người dùng")
    public UserResponse assignRoles(@PathVariable Long id, @Valid @RequestBody UserRoleAssignmentRequest request) {
        return userService.assignRoles(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa người dùng")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
