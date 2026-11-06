package com.example.roombooking.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.exception.BadRequestException;
import com.example.roombooking.exception.ResourceNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FileStorageUtils {

    public record StoredFile(String path, String storedName, String originalName, String contentType, long size) {}

    private final Path root;

    public FileStorageUtils(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    // ===================== Lưu file =====================

    /** Lưu ảnh: kiểm tra đuôi file, dung lượng và nội dung thật (magic bytes), không tin Content-Type của client. */
    public StoredFile saveImage(MultipartFile file, String subDir) {
        requireNotEmpty(file);
        if (file.getSize() > ApiConstants.MAX_IMAGE_SIZE_BYTES)
            throw new BadRequestException("Ảnh vượt quá dung lượng tối đa " + (ApiConstants.MAX_IMAGE_SIZE_BYTES / 1024 / 1024) + " MB");
        String ext = extensionOf(file);
        if (!ApiConstants.ALLOWED_IMAGE_EXTENSIONS.contains(ext))
            throw new BadRequestException("Chỉ chấp nhận ảnh định dạng: " + String.join(", ", ApiConstants.ALLOWED_IMAGE_EXTENSIONS));
        String detected = detectImageType(file);
        if (detected == null)
            throw new BadRequestException("Nội dung file không phải là ảnh hợp lệ");
        return store(file, subDir, ext, detected);
    }

    /** Lưu file đính kèm: kiểm tra đuôi file và dung lượng. */
    public StoredFile saveAttachment(MultipartFile file, String subDir) {
        requireNotEmpty(file);
        if (file.getSize() > ApiConstants.MAX_ATTACHMENT_SIZE_BYTES)
            throw new BadRequestException("File vượt quá dung lượng tối đa " + (ApiConstants.MAX_ATTACHMENT_SIZE_BYTES / 1024 / 1024) + " MB");
        String ext = extensionOf(file);
        if (!ApiConstants.ALLOWED_ATTACHMENT_EXTENSIONS.contains(ext))
            throw new BadRequestException("Định dạng file không được hỗ trợ: ." + ext);
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        return store(file, subDir, ext, contentType);
    }

    private StoredFile store(MultipartFile file, String subDir, String ext, String contentType) {
        try {
            Path dir = resolveSafe(subDir);
            Files.createDirectories(dir);
            String storedName = UUID.randomUUID() + "." + ext;
            Path target = dir.resolve(storedName).normalize();
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            cleanupOnRollback(target);
            String relative = root.relativize(target).toString().replace('\\', '/');
            return new StoredFile(relative, storedName, file.getOriginalFilename(), contentType, file.getSize());
        } catch (IOException e) {
            throw new IllegalStateException("Không thể lưu file lên máy chủ", e);
        }
    }

    // ===================== Đọc / xóa =====================

    public Resource load(String relativePath) {
        Path p = resolveSafe(relativePath);
        if (!Files.isRegularFile(p)) throw new ResourceNotFoundException("File không tồn tại trên máy chủ");
        return new FileSystemResource(p);
    }

    /** Xóa file sau khi transaction commit thành công (nếu rollback thì giữ nguyên file cũ). */
    public void deleteAfterCommit(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteQuietly(relativePath);
                }
            });
        } else {
            deleteQuietly(relativePath);
        }
    }

    public void deleteQuietly(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Files.deleteIfExists(resolveSafe(relativePath));
        } catch (IOException e) {
            log.warn("Không xóa được file {}: {}", relativePath, e.getMessage());
        }
    }

    // ===================== Helper =====================

    /** File vừa ghi ra đĩa nhưng transaction rollback thì xóa đi, tránh file mồ côi. */
    private void cleanupOnRollback(Path target) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        try {
                            Files.deleteIfExists(target);
                        } catch (IOException ignored) {
                            // bỏ qua
                        }
                    }
                }
            });
        }
    }

    /** Chặn path traversal: đường dẫn sau khi chuẩn hóa phải nằm trong thư mục gốc. */
    private Path resolveSafe(String relativePath) {
        Path p = root.resolve(relativePath).normalize();
        if (!p.startsWith(root)) throw new BadRequestException("Đường dẫn file không hợp lệ");
        return p;
    }

    private void requireNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("File không được để trống");
    }

    private String extensionOf(MultipartFile file) {
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext == null || ext.isBlank()) throw new BadRequestException("File phải có đuôi mở rộng (ví dụ .png, .pdf)");
        return ext.toLowerCase(Locale.ROOT);
    }

    private String detectImageType(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] h = in.readNBytes(12);
            if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return "image/jpeg";
            if (h.length >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return "image/png";
            if (h.length >= 4 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') return "image/gif";
            if (h.length >= 12 && new String(h, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(h, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) return "image/webp";
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}