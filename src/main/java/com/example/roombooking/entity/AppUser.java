package com.example.roombooking.entity;

import jakarta.persistence.*;
import com.example.roombooking.util.DateTimeUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        })
public class AppUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Nguồn ảnh đại diện: chưa có / dán link / tải file lên. */
    public enum AvatarSource { NONE, LINK, FILE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dùng để đăng nhập (đăng nhập được bằng username hoặc email). */
    @Column(nullable = false, length = 50)
    private String username;

    /** Dùng để đăng nhập, luôn lưu chữ thường. */
    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String department;

    // ===== Ảnh đại diện =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AvatarSource avatarSource = AvatarSource.NONE;

    /** Link ảnh (khi avatarSource = LINK). */
    @Column(length = 500)
    private String avatarUrl;

    /** Đường dẫn tương đối của file đã lưu trên đĩa (khi avatarSource = FILE). */
    @Column(length = 500)
    private String avatarPath;

    @Column(length = 100)
    private String avatarContentType;

    // ===== Trạng thái & phân quyền =====
    @Column(nullable = false)
    private boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    // ===== Thời gian =====
    private LocalDateTime lastLoginAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ===== Constructors =====

    public AppUser() {}

    private AppUser(Builder builder) {
        this.username = builder.username;
        this.email = builder.email;
        this.password = builder.password;
        this.fullName = builder.fullName;
        this.phone = builder.phone;
        this.department = builder.department;
        this.avatarSource = builder.avatarSource != null ? builder.avatarSource : AvatarSource.NONE;
        this.avatarUrl = builder.avatarUrl;
        this.avatarPath = builder.avatarPath;
        this.avatarContentType = builder.avatarContentType;
        this.enabled = builder.enabled;
        this.roles = builder.roles != null ? builder.roles : new HashSet<>();
    }

    // ===== Getters =====

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getDepartment() { return department; }
    public AvatarSource getAvatarSource() { return avatarSource; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getAvatarPath() { return avatarPath; }
    public String getAvatarContentType() { return avatarContentType; }
    public boolean isEnabled() { return enabled; }
    public Set<Role> getRoles() { return roles; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ===== Setters =====

    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setDepartment(String department) { this.department = department; }
    public void setAvatarSource(AvatarSource avatarSource) { this.avatarSource = avatarSource; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
    public void setAvatarContentType(String avatarContentType) { this.avatarContentType = avatarContentType; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ===== JPA Callbacks =====

    @PrePersist
    @SuppressWarnings("unused") // Invoked by the JPA lifecycle
    void onCreate() {
        LocalDateTime now = DateTimeUtils.now();
        this.createdAt = now;
        this.updatedAt = now;
        normalize();
    }

    @PreUpdate
    @SuppressWarnings("unused") // Invoked by the JPA lifecycle
    void onUpdate() {
        this.updatedAt = DateTimeUtils.now();
        normalize();
    }

    private void normalize() {
        if (this.email != null) this.email = this.email.trim().toLowerCase();
        if (this.username != null) this.username = this.username.trim();
        if (this.avatarSource == null) this.avatarSource = AvatarSource.NONE;
    }

    // ===== Helper methods =====

    /** Link ảnh để trả về cho FE: link ngoài hoặc đường dẫn file đã lưu. */
    public boolean hasAvatar() {
        return avatarSource != null && avatarSource != AvatarSource.NONE;
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.getName().equals(roleName));
    }

    // ===== Builder =====

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String username;
        private String email;
        private String password;
        private String fullName;
        private String phone;
        private String department;
        private AvatarSource avatarSource;
        private String avatarUrl;
        private String avatarPath;
        private String avatarContentType;
        private boolean enabled = true;
        private Set<Role> roles;

        public Builder username(String username) { this.username = username; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder department(String department) { this.department = department; return this; }
        public Builder avatarSource(AvatarSource avatarSource) { this.avatarSource = avatarSource; return this; }
        public Builder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public Builder avatarPath(String avatarPath) { this.avatarPath = avatarPath; return this; }
        public Builder avatarContentType(String avatarContentType) { this.avatarContentType = avatarContentType; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder roles(Set<Role> roles) { this.roles = roles; return this; }

        public AppUser build() { return new AppUser(this); }
    }
}
