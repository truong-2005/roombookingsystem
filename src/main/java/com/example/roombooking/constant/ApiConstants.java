package com.example.roombooking.constant;

import java.util.Set;

public final class ApiConstants {

    private ApiConstants() {}

    // ===== Đường dẫn gốc =====
    public static final String API_PREFIX = "/api";
    public static final String AUTH = API_PREFIX + "/auth";
    public static final String PROFILE = API_PREFIX + "/profile";
    public static final String USERS = API_PREFIX + "/users";
    public static final String ROLES = API_PREFIX + "/roles";
    public static final String ROOMS = API_PREFIX + "/rooms";
    public static final String BOOKINGS = API_PREFIX + "/bookings";
    public static final String NOTIFICATIONS = API_PREFIX + "/notifications";
    public static final String DASHBOARD = API_PREFIX + "/dashboard";
    public static final String FILES = API_PREFIX + "/files";

    // ===== Phân trang =====
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    // ===== Trường được phép sắp xếp (chống sort theo trường tùy ý) =====
    public static final Set<String> ROOM_SORT_FIELDS =
            Set.of("id", "code", "name", "type", "capacity", "createdAt");
    public static final Set<String> USER_SORT_FIELDS =
            Set.of("id", "username", "email", "fullName", "department", "createdAt");
    public static final Set<String> BOOKING_SORT_FIELDS =
            Set.of("id", "title", "startTime", "endTime", "status", "createdAt");
    public static final Set<String> NOTIFICATION_SORT_FIELDS =
            Set.of("id", "createdAt");

    // ===== Thư mục lưu file (tương đối so với app.upload.dir) =====
    public static final String UPLOAD_AVATAR_DIR = "avatars";
    public static final String UPLOAD_ROOM_DIR = "rooms";
    public static final String UPLOAD_BOOKING_DIR = "bookings";

    // ===== Giới hạn file =====
    public static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;         // 5 MB / ảnh
    public static final long MAX_ATTACHMENT_SIZE_BYTES = 20L * 1024 * 1024;   // 20 MB / file đính kèm
    public static final int MAX_ATTACHMENTS_PER_BOOKING = 20;

    public static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    public static final Set<String> ALLOWED_IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp", "gif");

    public static final Set<String> ALLOWED_ATTACHMENT_EXTENSIONS =
            Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt",
                    "jpg", "jpeg", "png", "zip");
}