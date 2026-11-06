package com.example.roombooking.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_attachments",
        indexes = @Index(name = "idx_booking_attachments_booking", columnList = "booking_id"))
public class BookingAttachment {

    /** Tải lên từng file riêng lẻ hay chọn cả thư mục. */
    public enum UploadSource { FILE, FOLDER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** Tên file gốc người dùng tải lên. */
    @Column(nullable = false, length = 255)
    private String originalFileName;

    /** Tên file lưu trên đĩa (UUID + đuôi file) để tránh trùng tên. */
    @Column(nullable = false, length = 255)
    private String storedFileName;

    /** Đường dẫn tương đối của file đã lưu, ví dụ: bookings/12/ab12cd.pdf */
    @Column(nullable = false, length = 500)
    private String filePath;

    /** Đường dẫn bên trong thư mục người dùng chọn, ví dụ: HoSo/BienBan/a.docx (chỉ có khi FOLDER). */
    @Column(length = 500)
    private String relativePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UploadSource uploadSource = UploadSource.FILE;

    @Column(length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private AppUser uploadedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    // ===== Constructors =====

    public BookingAttachment() {}

    private BookingAttachment(Builder builder) {
        this.booking = builder.booking;
        this.originalFileName = builder.originalFileName;
        this.storedFileName = builder.storedFileName;
        this.filePath = builder.filePath;
        this.relativePath = builder.relativePath;
        this.uploadSource = builder.uploadSource != null ? builder.uploadSource : UploadSource.FILE;
        this.contentType = builder.contentType;
        this.fileSize = builder.fileSize;
        this.uploadedBy = builder.uploadedBy;
    }

    // ===== Getters =====

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public String getOriginalFileName() { return originalFileName; }
    public String getStoredFileName() { return storedFileName; }
    public String getFilePath() { return filePath; }
    public String getRelativePath() { return relativePath; }
    public UploadSource getUploadSource() { return uploadSource; }
    public String getContentType() { return contentType; }
    public Long getFileSize() { return fileSize; }
    public AppUser getUploadedBy() { return uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }

    // ===== Setters =====

    public void setId(Long id) { this.id = id; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public void setStoredFileName(String storedFileName) { this.storedFileName = storedFileName; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }
    public void setUploadSource(UploadSource uploadSource) { this.uploadSource = uploadSource; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public void setUploadedBy(AppUser uploadedBy) { this.uploadedBy = uploadedBy; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    // ===== JPA Callbacks =====

    @PrePersist
    void onCreate() {
        this.uploadedAt = LocalDateTime.now();
        if (this.uploadSource == null) this.uploadSource = UploadSource.FILE;
    }

    // ===== Builder =====

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Booking booking;
        private String originalFileName;
        private String storedFileName;
        private String filePath;
        private String relativePath;
        private UploadSource uploadSource;
        private String contentType;
        private Long fileSize;
        private AppUser uploadedBy;

        public Builder booking(Booking booking) { this.booking = booking; return this; }
        public Builder originalFileName(String originalFileName) { this.originalFileName = originalFileName; return this; }
        public Builder storedFileName(String storedFileName) { this.storedFileName = storedFileName; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder relativePath(String relativePath) { this.relativePath = relativePath; return this; }
        public Builder uploadSource(UploadSource uploadSource) { this.uploadSource = uploadSource; return this; }
        public Builder contentType(String contentType) { this.contentType = contentType; return this; }
        public Builder fileSize(Long fileSize) { this.fileSize = fileSize; return this; }
        public Builder uploadedBy(AppUser uploadedBy) { this.uploadedBy = uploadedBy; return this; }

        public BookingAttachment build() { return new BookingAttachment(this); }
    }
}
