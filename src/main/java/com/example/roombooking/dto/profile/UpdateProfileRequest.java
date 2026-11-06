package com.example.roombooking.dto.profile;

import com.example.roombooking.entity.AppUser.AvatarSource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

/** Gửi dạng multipart/form-data. Ảnh: dán link (avatarUrl) HOẶC chọn file (avatarFile). */
public class UpdateProfileRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @Pattern(regexp = "^$|^(0|\\+84)\\d{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Size(max = 100, message = "Đơn vị tối đa 100 ký tự")
    private String department;

    @Size(max = 500, message = "Link ảnh tối đa 500 ký tự")
    @Pattern(regexp = "^$|^https?://\\S+$", message = "Link ảnh phải bắt đầu bằng http:// hoặc https://")
    private String avatarUrl;

    private MultipartFile avatarFile;

    /** true = xóa ảnh đại diện hiện tại. */
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
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getDepartment() { return department; }
    public String getAvatarUrl() { return avatarUrl; }
    public MultipartFile getAvatarFile() { return avatarFile; }
    public boolean isRemoveAvatar() { return removeAvatar; }

    // ===== Setters =====
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setDepartment(String department) { this.department = department; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setAvatarFile(MultipartFile avatarFile) { this.avatarFile = avatarFile; }
    public void setRemoveAvatar(boolean removeAvatar) { this.removeAvatar = removeAvatar; }
}