package com.example.roombooking.exception;

public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException() {
        super("Mật khẩu hiện tại không đúng");
    }

    public InvalidPasswordException(String message) {
        super(message);
    }
}