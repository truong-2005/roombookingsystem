package com.example.roombooking.entity;

/**
 * JPA Metamodel class cho Booking entity - chứa các field name constants
 * để sử dụng type-safe property references thay vì string literals.
 */
@SuppressWarnings("java:S101") // JPA Metamodel naming convention uses underscore suffix
public final class Booking_ {
    
    private Booking_() {
        // Utility class - không cho phép khởi tạo
    }
    
    // Field names
    public static final String ID = "id";
    public static final String ROOM = "room";
    public static final String USER = "user";
    public static final String TITLE = "title";
    public static final String PURPOSE = "purpose";
    public static final String ATTENDEES = "attendees";
    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endTime";
    public static final String STATUS = "status";
    public static final String REJECT_REASON = "rejectReason";
    public static final String REVIEWED_BY = "reviewedBy";
    public static final String REVIEWED_AT = "reviewedAt";
    public static final String CANCEL_REASON = "cancelReason";
    public static final String CANCELLED_AT = "cancelledAt";
    public static final String CREATED_AT = "createdAt";
    public static final String UPDATED_AT = "updatedAt";
    public static final String ATTACHMENTS = "attachments";
}
