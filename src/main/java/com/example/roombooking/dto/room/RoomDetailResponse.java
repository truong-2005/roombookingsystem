package com.example.roombooking.dto.room;

import com.example.roombooking.entity.Room.ImageSource;
import com.example.roombooking.entity.Room.RoomType;

import java.time.LocalDateTime;

public record RoomDetailResponse(
        Long id,
        String code,
        String name,
        RoomType type,
        Integer capacity,
        String location,
        String description,
        String amenities,
        ImageSource imageSource,
        String imageUrl,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}