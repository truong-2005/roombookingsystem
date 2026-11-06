package com.example.roombooking.service.impl;

import java.util.Locale;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.example.roombooking.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final TemplateEngine templateEngine;

    @java.beans.ConstructorProperties({"mailSenderProvider", "templateEngine"})
    public EmailServiceImpl(
        ObjectProvider<JavaMailSender> mailSenderProvider,
        TemplateEngine templateEngine
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.templateEngine = templateEngine;
    }

    /** false (mặc định): không gửi thật, chỉ ghi log nội dung email (tiện khi chạy local). */
    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${app.mail.from:no-reply@roombooking.local}")
    private String from;

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String fullName, String resetLink, int expiresInMinutes) {
        send(to, "Đặt lại mật khẩu - Room Booking System", "email/forgot-password",
                Map.of("fullName", fullName, "resetLink", resetLink, "expiresInMinutes", expiresInMinutes));
    }

    @Override
    @Async
    public void sendPasswordChangedEmail(String to, String fullName) {
        send(to, "Mật khẩu của bạn đã được thay đổi", "email/booking-notification",
                Map.of("fullName", fullName,
                        "title", "Mật khẩu đã được thay đổi",
                        "message", "Mật khẩu tài khoản của bạn vừa được thay đổi. "
                                + "Nếu không phải bạn thực hiện, hãy liên hệ quản trị viên ngay."));
    }

    @Override
    @Async
    public void sendBookingNotificationEmail(String to, String fullName, String title, String message) {
        send(to, title, "email/booking-notification",
                Map.of("fullName", fullName, "title", title, "message", message));
    }

    private void send(String to, String subject, String template, Map<String, Object> vars) {
        try {
            Context ctx = new Context(Locale.forLanguageTag("vi-VN"));
            ctx.setVariables(vars);
            String html = templateEngine.process(template, ctx);

            JavaMailSender sender = mailSenderProvider.getIfAvailable();
            if (!enabled || sender == null) {
                log.info("[EMAIL chưa bật gửi thật] tới={} | tiêu đề={} | dữ liệu={}", to, subject, vars);
                return;
            }
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(msg);
        } catch (MessagingException | BeansException | MailException e) {
            // Lỗi gửi mail không được làm hỏng nghiệp vụ chính
            log.error("Gửi email tới {} thất bại: {}", to, e.getMessage());
        }
    }
}