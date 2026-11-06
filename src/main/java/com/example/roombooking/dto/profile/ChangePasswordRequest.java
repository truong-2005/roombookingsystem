package com.example.roombooking.dto.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        String currentPassword,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, max = 100, message = "Mật khẩu mới phải từ 8 đến 100 ký tự")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Mật khẩu mới phải có ít nhất 1 chữ cái và 1 chữ số")
        String newPassword,

        @NotBlank(message = "Xác nhận mật khẩu không được để trống")
        String confirmPassword
) {
    @JsonIgnore
    @AssertTrue(message = "Mật khẩu xác nhận không khớp")
    public boolean isPasswordConfirmed() {
        return newPassword == null || newPassword.equals(confirmPassword);
    }

    @JsonIgnore
    @AssertTrue(message = "Mật khẩu mới phải khác mật khẩu hiện tại")
    public boolean isNewPasswordDifferent() {
        return newPassword == null || !newPassword.equals(currentPassword);
    }
}