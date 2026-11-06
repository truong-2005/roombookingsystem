package com.example.roombooking.dto.booking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record BookingCreateRequest(
        @NotNull(message = "Vui lòng chọn phòng")
        Long roomId,

        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
        String title,

        @Size(max = 1000, message = "Mục đích tối đa 1000 ký tự")
        String purpose,

        @Min(value = 1, message = "Số người tham dự phải lớn hơn 0")
        Integer attendees,

        @NotNull(message = "Thời gian bắt đầu không được để trống")
        @Future(message = "Thời gian bắt đầu phải ở tương lai")
        LocalDateTime startTime,

        @NotNull(message = "Thời gian kết thúc không được để trống")
        LocalDateTime endTime
) {
    @JsonIgnore
    @AssertTrue(message = "Thời gian kết thúc phải sau thời gian bắt đầu")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }

    @JsonIgnore
    @AssertTrue(message = "Lịch đặt phải bắt đầu và kết thúc trong cùng một ngày")
    public boolean isSameDay() {
        return startTime == null || endTime == null || startTime.toLocalDate().equals(endTime.toLocalDate());
    }
}