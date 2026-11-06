package com.example.roombooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.example.roombooking.constant.SecurityConstants;
import com.example.roombooking.security.JwtAuthenticationFilter;
import com.example.roombooking.security.RestAccessDeniedHandler;
import com.example.roombooking.security.RestAuthenticationEntryPoint;

import java.beans.ConstructorProperties;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // cho phép @PreAuthorize ở controller
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @ConstructorProperties({"jwtAuthenticationFilter", "authenticationEntryPoint", "accessDeniedHandler",
            "corsConfigurationSource"})
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())            // API stateless dùng Bearer token, không dùng cookie
                .cors(c -> c.configurationSource(corsConfigurationSource))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)   // 401
                        .accessDeniedHandler(accessDeniedHandler))            // 403
                .authorizeHttpRequests(auth -> auth
                        // Thứ tự quan trọng: quy tắc đầu tiên khớp sẽ được áp dụng
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Swagger và các API công khai (đăng nhập, đăng ký, quên mật khẩu, xem ảnh...)
                        .requestMatchers(SecurityConstants.SWAGGER_ENDPOINTS).permitAll()
                        .requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()

                        // Danh sách người dùng và vai trò chứa dữ liệu cá nhân: chỉ ADMIN được xem
                        .requestMatchers(HttpMethod.GET, "/api/users/**", "/api/roles/**").hasRole("ADMIN")

                        // Duyệt / từ chối lịch: chỉ ADMIN
                        .requestMatchers("/api/bookings/*/approve", "/api/bookings/*/reject",
                                "/api/bookings/*/decision").hasRole("ADMIN")

                        // Mọi tài khoản đã đăng nhập được GET
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()

                        // Nhóm cho phép USER ghi dữ liệu của chính mình (service kiểm tra quyền sở hữu)
                        .requestMatchers("/api/auth/**", "/api/profile/**", "/api/notifications/**",
                                "/api/bookings/**").authenticated()

                        // Còn lại (thêm / sửa / xóa phòng, người dùng, vai trò...): chỉ ADMIN
                        .requestMatchers("/api/**").hasRole("ADMIN")

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
