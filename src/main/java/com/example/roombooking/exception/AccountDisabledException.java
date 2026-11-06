package com.example.roombooking.exception;

public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException() {
        super("Tài khoản đã bị khóa, vui lòng liên hệ quản trị viên");
    }

    public AccountDisabledException(String message) {
        super(message);
    }
}