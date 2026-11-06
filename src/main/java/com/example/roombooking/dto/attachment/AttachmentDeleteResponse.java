package com.example.roombooking.dto.attachment;

public record AttachmentDeleteResponse(Long id, String fileName, boolean deleted, String message) {}