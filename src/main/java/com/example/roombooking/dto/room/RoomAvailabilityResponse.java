package com.example.roombooking.dto.room;

import com.example.roombooking.dto.booking.BookingResponse;

import java.time.LocalDateTime;
import java.util.List;

public record RoomAvailabilityResponse(
        Long roomId,
        String roomCode,
        String roomName,
        LocalDateTime start,
        LocalDateTime end,
        boolean available,
        List<BookingResponse> conflicts
) {}