package com.example.roombooking.dto.user;

import com.example.roombooking.entity.AppUser.AvatarSource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

/** ADMIN cập nhật người dùng, có thể đặt mật khẩu mới. Gửi dạng multipart/form-data. */
public class UserUpdateRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
            message = "Tên đăng nhập chỉ gồm chữ, số, dấu chấm, gạch dưới, gạch ngang và không chứa ký tự @")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @Pattern(regexp = "^$|^(0|\\+84)\\d{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Size(max = 100, message = "Đơn vị tối đa 100 ký tự")
    private String department;

    /** Nếu có giá trị thì đặt lại mật khẩu cho người dùng; để trống là giữ nguyên. */
    @Size(max = 100, message = "Mật khẩu tối đa 100 ký tự")
    @Pattern(regexp = "^$|^(?=.{8,100}$)(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Mật khẩu phải từ 8 đến 100 ký tự, gồm ít nhất 1 chữ cái và 1 chữ số")
    private String newPassword;

    @Size(max = 500, message = "Link ảnh tối đa 500 ký tự")
    @Pattern(regexp = "^$|^https?://\\S+$", message = "Link ảnh phải bắt đầu bằng http:// hoặc https://")
    private String avatarUrl;

    private MultipartFile avatarFile;

    private boolean removeAvatar;

    @JsonIgnore
    @AssertTrue(message = "Chỉ chọn một: dán link ảnh hoặc tải file ảnh lên")
    public boolean isAvatarSourceValid() {
        boolean hasUrl = avatarUrl != null && !avatarUrl.isBlank();
        boolean hasFile = avatarFile != null && !avatarFile.isEmpty();
        return !(hasUrl && hasFile);
    }

    public AvatarSource resolveAvatarSource() {
        if (avatarFile != null && !avatarFile.isEmpty()) return AvatarSource.FILE;
        if (avatarUrl != null && !avatarUrl.isBlank()) return AvatarSource.LINK;
        return AvatarSource.NONE;
    }

    // ===== Getters =====
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getDepartment() { return department; }
    public String getNewPassword() { return newPassword; }
    public String getAvatarUrl() { return avatarUrl; }
    public MultipartFile getAvatarFile() { return avatarFile; }
    public boolean isRemoveAvatar() { return removeAvatar; }

    // ===== Setters =====
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setDepartment(String department) { this.department = department; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setAvatarFile(MultipartFile avatarFile) { this.avatarFile = avatarFile; }
    public void setRemoveAvatar(boolean removeAvatar) { this.removeAvatar = removeAvatar; }
}
