package com.example.roombooking.security;

import com.example.roombooking.repository.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Triển khai {@link UserDetailsService} để Spring Security có thể load người dùng
 * khi xác thực JWT filter.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    @java.beans.ConstructorProperties({"userRepository"})
    public CustomUserDetailsService(
        AppUserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    /**
     * Load user theo username. Được JWT filter gọi sau khi parse token thành công.
     *
     * @param username tên đăng nhập (subject trong JWT)
     * @throws UsernameNotFoundException nếu không tìm thấy
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameIgnoreCase(username)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy người dùng: " + username));
    }
}
