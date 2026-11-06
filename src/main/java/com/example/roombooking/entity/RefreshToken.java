package com.example.roombooking.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_tokens_token", columnNames = "token"),
        indexes = @Index(name = "idx_refresh_tokens_user", columnList = "user_id"))
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(length = 255)
    private String userAgent;

    @Column(length = 64)
    private String ipAddress;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ===== Constructors =====

    public RefreshToken() {}

    private RefreshToken(Builder builder) {
        this.token = builder.token;
        this.user = builder.user;
        this.expiryDate = builder.expiryDate;
        this.revoked = builder.revoked;
        this.userAgent = builder.userAgent;
        this.ipAddress = builder.ipAddress;
    }

    // ===== Getters =====

    public Long getId() { return id; }
    public String getToken() { return token; }
    public AppUser getUser() { return user; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public boolean isRevoked() { return revoked; }
    public String getUserAgent() { return userAgent; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ===== Setters =====

    public void setId(Long id) { this.id = id; }
    public void setToken(String token) { this.token = token; }
    public void setUser(AppUser user) { this.user = user; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ===== JPA Callbacks =====

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ===== Helper methods =====

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDateTime.now());
    }

    public boolean isUsable() {
        return !revoked && !isExpired();
    }

    // ===== Builder =====

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String token;
        private AppUser user;
        private LocalDateTime expiryDate;
        private boolean revoked = false;
        private String userAgent;
        private String ipAddress;

        public Builder token(String token) { this.token = token; return this; }
        public Builder user(AppUser user) { this.user = user; return this; }
        public Builder expiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; return this; }
        public Builder revoked(boolean revoked) { this.revoked = revoked; return this; }
        public Builder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public Builder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }

        public RefreshToken build() { return new RefreshToken(this); }
    }
}
