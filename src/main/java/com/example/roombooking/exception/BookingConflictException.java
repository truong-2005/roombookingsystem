package com.example.roombooking.exception;

import com.example.roombooking.dto.booking.BookingConflictResponse.ConflictItem;

import java.util.List;

public class BookingConflictException extends RuntimeException {

    private final transient List<ConflictItem> conflicts;

    public BookingConflictException(String message) {
        super(message);
        this.conflicts = List.of();
    }

    public BookingConflictException(String message, List<ConflictItem> conflicts) {
        super(message);
        this.conflicts = conflicts == null ? List.of() : conflicts;
    }

    public List<ConflictItem> getConflicts() {
        return conflicts;
    }
}