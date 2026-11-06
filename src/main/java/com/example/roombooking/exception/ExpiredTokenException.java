package com.example.roombooking.exception;

public class ExpiredTokenException extends RuntimeException {

    public ExpiredTokenException() {
        super("Mã xác thực đã hết hạn, vui lòng yêu cầu lại");
    }

    public ExpiredTokenException(String message) {
        super(message);
    }
}