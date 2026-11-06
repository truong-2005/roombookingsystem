package com.example.roombooking.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Bean JavaMailSender do Spring Boot tự tạo khi có spring.mail.host trong application.properties,
 * nên không khai báo lại ở đây (EmailServiceImpl dùng ObjectProvider nên chạy được cả khi chưa cấu hình mail).
 * File này bật @Async (gửi email không chặn request) và @Scheduled (dọn token, thông báo cũ).
 */
@Configuration
@EnableAsync
@EnableScheduling
public class MailConfig {

    /** Tên "taskExecutor" là tên mặc định @Async tìm đến. */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(5);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("async-");
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(20);
        ex.initialize();
        return ex;
    }
}