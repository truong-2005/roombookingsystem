package com.example.roombooking.dto.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookingCancelRequest(
        @NotBlank(message = "Vui lòng nhập lý do hủy")
        @Size(max = 500, message = "Lý do hủy tối đa 500 ký tự")
        String reason
) {}