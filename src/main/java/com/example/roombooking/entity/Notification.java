package com.example.roombooking.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications",
        indexes = @Index(name = "idx_notifications_user_read", columnList = "user_id, is_read"))
public class Notification {

    public enum NotificationType {
        BOOKING_CREATED, BOOKING_APPROVED, BOOKING_REJECTED,
        BOOKING_CANCELLED, BOOKING_RESCHEDULED, SYSTEM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Người nhận thông báo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** Lịch đặt liên quan (có thể không có với thông báo hệ thống). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    // "read" là từ khóa của MySQL nên đặt tên cột là is_read
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    private LocalDateTime readAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ===== Constructors =====

    public Notification() {}

    private Notification(Builder builder) {
        this.user = builder.user;
        this.booking = builder.booking;
        this.type = builder.type;
        this.title = builder.title;
        this.message = builder.message;
        this.read = builder.read;
    }

    // ===== Getters =====

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public Booking getBooking() { return booking; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public LocalDateTime getReadAt() { return readAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ===== Setters =====

    public void setId(Long id) { this.id = id; }
    public void setUser(AppUser user) { this.user = user; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public void setType(NotificationType type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setRead(boolean read) { this.read = read; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ===== JPA Callbacks =====

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ===== Helper methods =====

    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }

    // ===== Builder =====

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private AppUser user;
        private Booking booking;
        private NotificationType type;
        private String title;
        private String message;
        private boolean read = false;

        public Builder user(AppUser user) { this.user = user; return this; }
        public Builder booking(Booking booking) { this.booking = booking; return this; }
        public Builder type(NotificationType type) { this.type = type; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder read(boolean read) { this.read = read; return this; }

        public Notification build() { return new Notification(this); }
    }
}
