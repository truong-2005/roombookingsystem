package com.example.roombooking.dto.attachment;

import com.example.roombooking.entity.BookingAttachment.UploadSource;

import java.time.LocalDateTime;

public record AttachmentResponse(
        Long id,
        Long bookingId,
        String originalFileName,
        String contentType,
        Long fileSize,
        UploadSource uploadSource,
        String relativePath,
        String downloadUrl,
        String uploadedByName,
        LocalDateTime uploadedAt
) {}