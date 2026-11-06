package com.example.roombooking.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "Tên đăng nhập chỉ gồm chữ, số, dấu chấm, gạch dưới, gạch ngang và không chứa ký tự @")
        String username,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 100, message = "Email tối đa 100 ký tự")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 đến 100 ký tự")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Mật khẩu phải có ít nhất 1 chữ cái và 1 chữ số")
        String password,

        @NotBlank(message = "Xác nhận mật khẩu không được để trống")
        String confirmPassword,

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String fullName,

        @Pattern(regexp = "^$|^(0|\\+84)\\d{9,10}$", message = "Số điện thoại không hợp lệ")
        String phone,

        @Size(max = 100, message = "Đơn vị tối đa 100 ký tự")
        String department,

        @Size(max = 500, message = "Link ảnh tối đa 500 ký tự")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "Link ảnh phải bắt đầu bằng http:// hoặc https://")
        String avatarUrl,

        @NotBlank(message = "Vui lòng tải và nhập CAPTCHA")
        String captchaId,

        @NotBlank(message = "Vui lòng nhập đáp án CAPTCHA")
        String captchaAnswer
) {
    @JsonIgnore
    @AssertTrue(message = "Mật khẩu xác nhận không khớp")
    public boolean isPasswordConfirmed() {
        return password == null || password.equals(confirmPassword);
    }
}
