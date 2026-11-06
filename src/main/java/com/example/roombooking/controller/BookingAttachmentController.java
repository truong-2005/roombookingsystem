package com.example.roombooking.controller;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.dto.attachment.AttachmentDeleteResponse;
import com.example.roombooking.dto.attachment.AttachmentResponse;
import com.example.roombooking.entity.BookingAttachment.UploadSource;
import com.example.roombooking.service.BookingAttachmentService;
import com.example.roombooking.service.BookingAttachmentService.AttachmentFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.BOOKINGS + "/{bookingId}/attachments")
@Tag(name = "Booking attachments", description = "Tải lên, tải xuống và xóa tệp đính kèm lịch đặt")
public class BookingAttachmentController {

    private final BookingAttachmentService attachmentService;

    public BookingAttachmentController(BookingAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải tệp đính kèm lên", description = "Gửi nhiều phần files. source=FOLDER cho phép kèm relativePaths theo thứ tự tệp.")
    public List<AttachmentResponse> upload(
            @PathVariable Long bookingId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) UploadSource source,
            @RequestParam(required = false) List<String> relativePaths) {
        return attachmentService.upload(bookingId, files, source, relativePaths);
    }

    @GetMapping
    @Operation(summary = "Danh sách tệp đính kèm")
    public List<AttachmentResponse> list(@PathVariable Long bookingId) {
        return attachmentService.list(bookingId);
    }

    @GetMapping("/{attachmentId}/download")
    @Operation(summary = "Tải tệp đính kèm xuống")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            @PathVariable Long bookingId, @PathVariable Long attachmentId) {
        AttachmentFile file = attachmentService.download(bookingId, attachmentId);
        MediaType contentType = MediaType.parseMediaType(file.contentType());
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(file.resource());
    }

    @DeleteMapping("/{attachmentId}")
    @Operation(summary = "Xóa tệp đính kèm")
    public AttachmentDeleteResponse delete(@PathVariable Long bookingId, @PathVariable Long attachmentId) {
        return attachmentService.delete(bookingId, attachmentId);
    }
}
