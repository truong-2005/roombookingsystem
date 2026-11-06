package com.example.roombooking.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn");
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}