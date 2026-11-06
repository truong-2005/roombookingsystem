package com.example.roombooking.controller;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.dto.notification.NotificationListResponse;
import com.example.roombooking.dto.notification.NotificationReadResponse;
import com.example.roombooking.dto.notification.NotificationResponse;
import com.example.roombooking.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.NOTIFICATIONS)
@Validated
@Tag(name = "Notifications", description = "Thông báo của tài khoản đang đăng nhập")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Danh sách thông báo", description = "Phân trang thông báo của tôi; có thể chỉ lấy thông báo chưa đọc.")
    public NotificationListResponse getMyNotifications(
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return notificationService.getMyNotifications(unreadOnly, page, size);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Số thông báo chưa đọc")
    public long countUnread() {
        return notificationService.countUnread();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết thông báo")
    public NotificationResponse getById(@PathVariable Long id) {
        return notificationService.getById(id);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu thông báo đã đọc")
    public NotificationReadResponse markAsRead(@PathVariable Long id) {
        return notificationService.markAsRead(id);
    }

    @PatchMapping("/read")
    @Operation(summary = "Đánh dấu một nhóm thông báo đã đọc", description = "Body là mảng ID thông báo.")
    public NotificationReadResponse markSelectedAsRead(@NotEmpty @RequestBody List<Long> ids) {
        return notificationService.markSelectedAsRead(ids);
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Đánh dấu tất cả thông báo đã đọc")
    public NotificationReadResponse markAllAsRead() {
        return notificationService.markAllAsRead();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa thông báo")
    public void delete(@PathVariable Long id) {
        notificationService.delete(id);
    }

    @DeleteMapping("/read")
    @Operation(summary = "Xóa tất cả thông báo đã đọc")
    public int deleteAllRead() {
        return notificationService.deleteAllRead();
    }

}
