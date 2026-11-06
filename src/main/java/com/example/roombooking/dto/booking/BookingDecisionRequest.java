package com.example.roombooking.dto.booking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** ADMIN duyệt (approved = true) hoặc từ chối (approved = false, bắt buộc có lý do). */
public record BookingDecisionRequest(
        @NotNull(message = "Vui lòng chọn duyệt hoặc từ chối")
        Boolean approved,

        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        String reason
) {
    @JsonIgnore
    @AssertTrue(message = "Vui lòng nhập lý do khi từ chối lịch đặt")
    public boolean isReasonProvidedWhenRejected() {
        return approved == null || approved || (reason != null && !reason.isBlank());
    }
}