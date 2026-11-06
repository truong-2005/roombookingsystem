package com.example.roombooking.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.DateTimeFeature;

import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                .defaultTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
                // Ngày giờ ra dạng "2026-10-16T08:00:00" thay vì mảng số
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                // FE gửi thừa trường thì bỏ qua, không báo lỗi
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // Enum nhận cả "approved" lẫn "APPROVED"
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    }
}
