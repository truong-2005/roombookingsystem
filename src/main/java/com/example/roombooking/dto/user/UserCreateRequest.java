package com.example.roombooking.dto.user;

import com.example.roombooking.entity.AppUser.AvatarSource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Set;

/** ADMIN tạo người dùng. Gửi dạng multipart/form-data. */
public class UserCreateRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
            message = "Tên đăng nhập chỉ gồm chữ, số, dấu chấm, gạch dưới, gạch ngang và không chứa ký tự @")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 đến 100 ký tự")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Mật khẩu phải có ít nhất 1 chữ cái và 1 chữ số")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @Pattern(regexp = "^$|^(0|\\+84)\\d{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Size(max = 100, message = "Đơn vị tối đa 100 ký tự")
    private String department;

    /** Tên role, ví dụ ROLE_USER. Bỏ trống thì mặc định ROLE_USER. */
    private Set<@NotBlank(message = "Tên vai trò không được để trống") String> roles = new HashSet<>();

    private boolean enabled = true;

    @Size(max = 500, message = "Link ảnh tối đa 500 ký tự")
    @Pattern(regexp = "^$|^https?://\\S+$", message = "Link ảnh phải bắt đầu bằng http:// hoặc https://")
    private String avatarUrl;

    private MultipartFile avatarFile;

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
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getDepartment() { return department; }
    public Set<String> getRoles() { return roles; }
    public boolean isEnabled() { return enabled; }
    public String getAvatarUrl() { return avatarUrl; }
    public MultipartFile getAvatarFile() { return avatarFile; }

    // ===== Setters =====
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setDepartment(String department) { this.department = department; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setAvatarFile(MultipartFile avatarFile) { this.avatarFile = avatarFile; }
}