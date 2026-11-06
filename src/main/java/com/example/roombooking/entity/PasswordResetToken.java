package com.example.roombooking.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_password_reset_tokens_token", columnNames = "token"),
        indexes = @Index(name = "idx_password_reset_tokens_user", columnList = "user_id"))
public class PasswordResetToken {

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
    private boolean used = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ===== Constructors =====

    public PasswordResetToken() {}

    private PasswordResetToken(Builder builder) {
        this.token = builder.token;
        this.user = builder.user;
        this.expiryDate = builder.expiryDate;
        this.used = builder.used;
    }

    // ===== Getters =====

    public Long getId() { return id; }
    public String getToken() { return token; }
    public AppUser getUser() { return user; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public boolean isUsed() { return used; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ===== Setters =====

    public void setId(Long id) { this.id = id; }
    public void setToken(String token) { this.token = token; }
    public void setUser(AppUser user) { this.user = user; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    public void setUsed(boolean used) { this.used = used; }
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
        return !used && !isExpired();
    }

    // ===== Builder =====

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String token;
        private AppUser user;
        private LocalDateTime expiryDate;
        private boolean used = false;

        public Builder token(String token) { this.token = token; return this; }
        public Builder user(AppUser user) { this.user = user; return this; }
        public Builder expiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; return this; }
        public Builder used(boolean used) { this.used = used; return this; }

        public PasswordResetToken build() { return new PasswordResetToken(this); }
    }
}
