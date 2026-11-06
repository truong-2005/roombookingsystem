package com.example.roombooking.controller;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.exception.BadRequestException;
import com.example.roombooking.util.FileStorageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(ApiConstants.FILES + "/public")
@SecurityRequirements
@Tag(name = "Files", description = "Tệp ảnh công khai đã tải lên")
public class FileController {

    private final FileStorageUtils fileStorage;

    public FileController(FileStorageUtils fileStorage) {
        this.fileStorage = fileStorage;
    }

    @GetMapping("/avatars/{fileName:.+}")
    @Operation(summary = "Xem ảnh đại diện đã tải lên")
    public ResponseEntity<Resource> getAvatar(@PathVariable String fileName) {
        return image(ApiConstants.UPLOAD_AVATAR_DIR, fileName);
    }

    @GetMapping("/rooms/{fileName:.+}")
    @Operation(summary = "Xem ảnh phòng đã tải lên")
    public ResponseEntity<Resource> getRoomImage(@PathVariable String fileName) {
        return image(ApiConstants.UPLOAD_ROOM_DIR, fileName);
    }

    private ResponseEntity<Resource> image(String directory, String fileName) {
        if (fileName == null || fileName.isBlank() || fileName.contains("/")
                || fileName.contains("\\") || fileName.equals(".") || fileName.equals("..")) {
            throw new BadRequestException("Tên file không hợp lệ");
        }
        Resource resource = fileStorage.load(directory + "/" + fileName);
        MediaType contentType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .contentType(contentType)
                .body(resource);
    }
}
