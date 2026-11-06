package com.example.roombooking.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super("Không tìm thấy " + resource + " với id = " + id);
    }

    public ResourceNotFoundException(String resource, String field, Object value) {
        super("Không tìm thấy " + resource + " với " + field + " = " + value);
    }
}