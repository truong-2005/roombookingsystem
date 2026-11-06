package com.example.roombooking.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Value("${server.port:8080}")
    private int port;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Room Booking System API")
                        .version("1.0.0")
                        .description("""
                                API quản lý đăng ký và sử dụng phòng họp.

                                Các đường dẫn và schema được Springdoc sinh tự động từ các controller Spring MVC.

                                Luồng đăng nhập:
                                1. Gọi GET /api/auth/captcha để lấy câu hỏi CAPTCHA.
                                2. Gọi POST /api/auth/login (hoặc /api/auth/admin/login) với thông tin đăng nhập và CAPTCHA.
                                3. Với API được bảo vệ, bấm **Authorize** và dán accessToken (không cần gõ chữ Bearer).
                                4. Khi access token hết hạn, gọi POST /api/auth/refresh-token với refreshToken.\
                                """)
                        .contact(new Contact().name("Room Booking Team")))
                .servers(List.of(new Server().url("http://localhost:" + port).description("Local")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Dán accessToken, không cần gõ 'Bearer '")))
                // Áp dụng Bearer cho tất cả API, không cần gắn @SecurityRequirement ở từng controller
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
