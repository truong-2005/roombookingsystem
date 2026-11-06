package com.example.roombooking.service;

import com.example.roombooking.dto.auth.ForgotPasswordRequest;
import com.example.roombooking.dto.auth.ForgotPasswordResponse;
import com.example.roombooking.dto.auth.ResetPasswordRequest;
import com.example.roombooking.dto.auth.ResetPasswordResponse;
import com.example.roombooking.dto.auth.VerifyResetTokenRequest;

public interface PasswordResetService {

    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest req);

    /** Kiểm tra mã còn dùng được không (FE gọi khi mở trang đặt lại mật khẩu). */
    ResetPasswordResponse verifyToken(VerifyResetTokenRequest req);

    ResetPasswordResponse resetPassword(ResetPasswordRequest req);

    void purgeExpired();
}