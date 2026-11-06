package com.example.roombooking.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("Mã xác thực không hợp lệ hoặc đã được sử dụng");
    }

    public InvalidTokenException(String message) {
        super(message);
    }
}