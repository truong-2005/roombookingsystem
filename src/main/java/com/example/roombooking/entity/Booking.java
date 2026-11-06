package com.example.roombooking.entity;

import jakarta.persistence.*;
import com.example.roombooking.util.DateTimeUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings",
        indexes = {
                @Index(name = "idx_bookings_room_time", columnList = "room_id, start_time, end_time"),
                @Index(name = "idx_bookings_user", columnList = "user_id"),
                @Index(name = "idx_bookings_status", columnList = "status")
        })
public class Booking {

    public enum BookingStatus { PENDING, APPROVED, REJECTED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    /** Người đăng ký. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String purpose;

    private Integer attendees;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    // ===== Duyệt / từ chối =====
    @Column(length = 500)
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private AppUser reviewedBy;

    private LocalDateTime reviewedAt;

    // ===== Hủy =====
    @Column(length = 500)
    private String cancelReason;

    private LocalDateTime cancelledAt;

    // ===== File đính kèm =====
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingAttachment> attachments = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ===== Constructors =====

    public Booking() {}

    private Booking(Builder builder) {
        this.room = builder.room;
        this.user = builder.user;
        this.title = builder.title;
        this.purpose = builder.purpose;
        this.attendees = builder.attendees;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.status = builder.status != null ? builder.status : BookingStatus.PENDING;
    }

    // ===== Getters =====

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public AppUser getUser() { return user; }
    public String getTitle() { return title; }
    public String getPurpose() { return purpose; }
    public Integer getAttendees() { return attendees; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public BookingStatus getStatus() { return status; }
    public String getRejectReason() { return rejectReason; }
    public AppUser getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getCancelReason() { return cancelReason; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public List<BookingAttachment> getAttachments() { return attachments; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ===== Setters =====

    public void setId(Long id) { this.id = id; }
    public void setRoom(Room room) { this.room = room; }
    public void setUser(AppUser user) { this.user = user; }
    public void setTitle(String title) { this.title = title; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public void setAttendees(Integer attendees) { this.attendees = attendees; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public void setReviewedBy(AppUser reviewedBy) { this.reviewedBy = reviewedBy; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public void setAttachments(List<BookingAttachment> attachments) { this.attachments = attachments; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ===== JPA Callbacks =====

    @PrePersist
    @SuppressWarnings("unused") // Used by JPA lifecycle
    void onCreate() {
        LocalDateTime now = DateTimeUtils.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = BookingStatus.PENDING;
    }

    @PreUpdate
    @SuppressWarnings("unused") // Used by JPA lifecycle
    void onUpdate() {
        this.updatedAt = DateTimeUtils.now();
    }

    // ===== Helper methods =====

    public void addAttachment(BookingAttachment attachment) {
        attachments.add(attachment);
        attachment.setBooking(this);
    }

    public void removeAttachment(BookingAttachment attachment) {
        attachments.remove(attachment);
        attachment.setBooking(null);
    }

    // ===== Builder =====

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Room room;
        private AppUser user;
        private String title;
        private String purpose;
        private Integer attendees;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BookingStatus status;

        public Builder room(Room room) { this.room = room; return this; }
        public Builder user(AppUser user) { this.user = user; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder purpose(String purpose) { this.purpose = purpose; return this; }
        public Builder attendees(Integer attendees) { this.attendees = attendees; return this; }
        public Builder startTime(LocalDateTime startTime) { this.startTime = startTime; return this; }
        public Builder endTime(LocalDateTime endTime) { this.endTime = endTime; return this; }
        public Builder status(BookingStatus status) { this.status = status; return this; }

        public Booking build() { return new Booking(this); }
    }
}
