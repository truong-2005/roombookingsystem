package com.example.roombooking.dto.booking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Dời lịch: đổi giờ (và tùy chọn đổi phòng). Lịch sẽ quay về PENDING để duyệt lại. */
public record BookingRescheduleRequest(
        Long newRoomId,

        @NotNull(message = "Thời gian bắt đầu mới không được để trống")
        @Future(message = "Thời gian bắt đầu mới phải ở tương lai")
        LocalDateTime newStartTime,

        @NotNull(message = "Thời gian kết thúc mới không được để trống")
        LocalDateTime newEndTime,

        @Size(max = 500, message = "Lý do dời lịch tối đa 500 ký tự")
        String reason
) {
    @JsonIgnore
    @AssertTrue(message = "Thời gian kết thúc mới phải sau thời gian bắt đầu mới")
    public boolean isTimeRangeValid() {
        return newStartTime == null || newEndTime == null || newEndTime.isAfter(newStartTime);
    }

    @JsonIgnore
    @AssertTrue(message = "Lịch đặt phải bắt đầu và kết thúc trong cùng một ngày")
    public boolean isSameDay() {
        return newStartTime == null || newEndTime == null
                || newStartTime.toLocalDate().equals(newEndTime.toLocalDate());
    }
}