package com.example.roombooking.security;

import com.example.roombooking.entity.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapter bọc {@link AppUser} thành UserDetails chuẩn của Spring Security.
 */
public class CustomUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final AppUser user;
    private final Set<GrantedAuthority> authorities;

    public CustomUserDetails(AppUser user) {
        this.user = user;
        this.authorities = user.getRoles().stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r.getName()))
                .collect(Collectors.toUnmodifiableSet());
    }

    // ===== Thông tin bổ sung từ AppUser =====

    public Long getUserId() {
        return user.getId();
    }

    public AppUser getUser() {
        return user;
    }

    // ===== UserDetails interface =====

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /** Trả về username — dùng làm subject trong JWT. */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    @Override
    public boolean isAccountNonExpired()  { return true; }

    @Override
    public boolean isAccountNonLocked()   { return user.isEnabled(); }

    @Override
    public boolean isCredentialsNonExpired() { return true; }
}
