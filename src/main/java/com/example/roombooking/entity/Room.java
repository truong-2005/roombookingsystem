package com.example.roombooking.entity;

import java.time.LocalDateTime;

import com.example.roombooking.util.DateTimeUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "rooms",
        uniqueConstraints = @UniqueConstraint(name = "uk_rooms_code", columnNames = "code"))
public class Room {

    public enum RoomType { ROOM, CLASSROOM, LAB, HALL }

    /** Nguồn ảnh phòng: chưa có / dán link / tải file lên. */
    public enum ImageSource { NONE, LINK, FILE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomType type;

    @Column(nullable = false)
    private Integer capacity;

    @Column(length = 150)
    private String location;

    @Column(length = 1000)
    private String description;

    /** Tiện nghi, ví dụ: "Máy chiếu, Điều hòa, Micro". */
    @Column(length = 500)
    private String amenities;

    // ===== Ảnh phòng =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ImageSource imageSource = ImageSource.NONE;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String imagePath;

    @Column(length = 100)
    private String imageContentType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ===== Constructors =====

    public Room() {}

    private Room(Builder builder) {
        this.code = builder.code;
        this.name = builder.name;
        this.type = builder.type;
        this.capacity = builder.capacity;
        this.location = builder.location;
        this.description = builder.description;
        this.amenities = builder.amenities;
        this.imageSource = builder.imageSource != null ? builder.imageSource : ImageSource.NONE;
        this.imageUrl = builder.imageUrl;
        this.imagePath = builder.imagePath;
        this.imageContentType = builder.imageContentType;
        this.active = builder.active;
    }

    // ===== Getters =====

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public RoomType getType() { return type; }
    public Integer getCapacity() { return capacity; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public String getAmenities() { return amenities; }
    public ImageSource getImageSource() { return imageSource; }
    public String getImageUrl() { return imageUrl; }
    public String getImagePath() { return imagePath; }
    public String getImageContentType() { return imageContentType; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ===== Setters =====

    public void setId(Long id) { this.id = id; }
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setType(RoomType type) { this.type = type; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public void setLocation(String location) { this.location = location; }
    public void setDescription(String description) { this.description = description; }
    public void setAmenities(String amenities) { this.amenities = amenities; }
    public void setImageSource(ImageSource imageSource) { this.imageSource = imageSource; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setImageContentType(String imageContentType) { this.imageContentType = imageContentType; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ===== JPA Callbacks =====

    @PrePersist
    @SuppressWarnings("unused") // Used by JPA lifecycle
    void onCreate() {
        LocalDateTime now = DateTimeUtils.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.imageSource == null) this.imageSource = ImageSource.NONE;
    }

    @PreUpdate
    @SuppressWarnings("unused") // Used by JPA lifecycle
    void onUpdate() {
        this.updatedAt = DateTimeUtils.now();
    }

    // ===== Builder =====

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String code;
        private String name;
        private RoomType type;
        private Integer capacity;
        private String location;
        private String description;
        private String amenities;
        private ImageSource imageSource;
        private String imageUrl;
        private String imagePath;
        private String imageContentType;
        private boolean active = true;

        public Builder code(String code) { this.code = code; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder type(RoomType type) { this.type = type; return this; }
        public Builder capacity(Integer capacity) { this.capacity = capacity; return this; }
        public Builder location(String location) { this.location = location; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder amenities(String amenities) { this.amenities = amenities; return this; }
        public Builder imageSource(ImageSource imageSource) { this.imageSource = imageSource; return this; }
        public Builder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public Builder imagePath(String imagePath) { this.imagePath = imagePath; return this; }
        public Builder imageContentType(String imageContentType) { this.imageContentType = imageContentType; return this; }
        public Builder active(boolean active) { this.active = active; return this; }

        public Room build() { return new Room(this); }
    }
}
