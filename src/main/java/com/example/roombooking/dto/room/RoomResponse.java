package com.example.roombooking.dto.room;

import com.example.roombooking.entity.Room.ImageSource;
import com.example.roombooking.entity.Room.RoomType;

public record RoomResponse(
        Long id,
        String code,
        String name,
        RoomType type,
        Integer capacity,
        String location,
        ImageSource imageSource,
        String imageUrl,
        boolean active
) {}