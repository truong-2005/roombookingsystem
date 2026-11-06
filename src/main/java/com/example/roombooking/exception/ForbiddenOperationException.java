package com.example.roombooking.exception;

public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException() {
        super("Bạn không có quyền thực hiện thao tác này");
    }

    public ForbiddenOperationException(String message) {
        super(message);
    }
}