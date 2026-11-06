package com.example.roombooking.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resource, String field, Object value) {
        super(resource + " với " + field + " '" + value + "' đã tồn tại");
    }
}