package com.example.roombooking.service.impl;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.dto.attachment.AttachmentDeleteResponse;
import com.example.roombooking.dto.attachment.AttachmentResponse;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.Booking.BookingStatus;
import com.example.roombooking.entity.BookingAttachment;
import com.example.roombooking.entity.BookingAttachment.UploadSource;
import com.example.roombooking.entity.Role;
import com.example.roombooking.exception.BadRequestException;
import com.example.roombooking.exception.ForbiddenOperationException;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.mapper.BookingMapper;
import com.example.roombooking.repository.BookingAttachmentRepository;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.service.BookingAttachmentService;
import com.example.roombooking.service.UserService;
import com.example.roombooking.util.FileStorageUtils;
import com.example.roombooking.util.FileStorageUtils.StoredFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookingAttachmentServiceImpl implements BookingAttachmentService {

    private final BookingRepository bookingRepository;
    private final BookingAttachmentRepository attachmentRepository;
    private final BookingMapper bookingMapper;
    private final UserService userService;
    private final FileStorageUtils fileStorage;

    @java.beans.ConstructorProperties({"bookingRepository", "attachmentRepository", "bookingMapper", "userService", "fileStorage"})
    public BookingAttachmentServiceImpl(
        BookingRepository bookingRepository,
        BookingAttachmentRepository attachmentRepository,
        BookingMapper bookingMapper,
        UserService userService,
        FileStorageUtils fileStorage
    ) {
        this.bookingRepository = bookingRepository;
        this.attachmentRepository = attachmentRepository;
        this.bookingMapper = bookingMapper;
        this.userService = userService;
        this.fileStorage = fileStorage;
    }

    // ===================== Thêm =====================

    @Override
    @Transactional
    public List<AttachmentResponse> upload(Long bookingId, List<MultipartFile> files,
                                           UploadSource source, List<String> relativePaths) {
        AppUser current = userService.getCurrentUserEntity();
        Booking booking = getBooking(bookingId);
        assertOwnerOrAdmin(booking, current);
        validateBookingStatus(booking);

        List<MultipartFile> validFiles = validateAndGetFiles(files, bookingId);
        
        UploadSource uploadSource = source == null ? UploadSource.FILE : source;
        String dir = ApiConstants.UPLOAD_BOOKING_DIR + "/" + bookingId;
        
        return processFileUploads(validFiles, relativePaths, uploadSource, dir, booking, current);
    }
    
    private void validateBookingStatus(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.APPROVED) {
            throw new BadRequestException("Không thể thêm file vào lịch ở trạng thái hiện tại");
        }
    }
    
    private List<MultipartFile> validateAndGetFiles(List<MultipartFile> files, Long bookingId) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất một file");
        }
        
        long incoming = files.stream().filter(f -> f != null && !f.isEmpty()).count();
        if (incoming == 0) {
            throw new BadRequestException("Vui lòng chọn ít nhất một file");
        }
        
        if (attachmentRepository.countByBookingId(bookingId) + incoming > ApiConstants.MAX_ATTACHMENTS_PER_BOOKING) {
            throw new BadRequestException("Mỗi lịch đặt chỉ được đính kèm tối đa "
                    + ApiConstants.MAX_ATTACHMENTS_PER_BOOKING + " file");
        }
        
        return files;
    }
    
    private List<AttachmentResponse> processFileUploads(List<MultipartFile> files, List<String> relativePaths,
                                                        UploadSource uploadSource, String dir, 
                                                        Booking booking, AppUser current) {
        List<AttachmentResponse> result = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file == null || file.isEmpty()) continue;

            AttachmentResponse response = uploadSingleFile(file, i, relativePaths, uploadSource, dir, booking, current);
            result.add(response);
        }
        return result;
    }
    
    private AttachmentResponse uploadSingleFile(MultipartFile file, int index, List<String> relativePaths,
                                                UploadSource uploadSource, String dir, 
                                                Booking booking, AppUser current) {
        String originalName = sanitizeName(file.getOriginalFilename());
        String relative = null;
        
        if (uploadSource == UploadSource.FOLDER) {
            String given = relativePaths != null && index < relativePaths.size() ? relativePaths.get(index) : null;
            relative = sanitizeRelativePath(given, originalName);
        }

        StoredFile stored = fileStorage.saveAttachment(file, dir);
        BookingAttachment attachment = BookingAttachment.builder()
                .booking(booking)
                .originalFileName(originalName)
                .storedFileName(stored.storedName())
                .filePath(stored.path())
                .relativePath(relative)
                .uploadSource(uploadSource)
                .contentType(stored.contentType())
                .fileSize(stored.size())
                .uploadedBy(current)
                .build();
        return bookingMapper.toAttachmentResponse(attachmentRepository.save(attachment));
    }

    // ===================== Xem / tải =====================

    @Override
    public List<AttachmentResponse> list(Long bookingId) {
        Booking booking = getBooking(bookingId);
        assertOwnerOrAdmin(booking, userService.getCurrentUserEntity());
        return attachmentRepository.findByBookingIdOrderByUploadedAtAsc(bookingId).stream()
                .map(bookingMapper::toAttachmentResponse).toList();
    }

    @Override
    public AttachmentFile download(Long bookingId, Long attachmentId) {
        BookingAttachment a = getAttachment(bookingId, attachmentId);
        assertOwnerOrAdmin(a.getBooking(), userService.getCurrentUserEntity());
        return new AttachmentFile(fileStorage.load(a.getFilePath()), a.getOriginalFileName(),
                a.getContentType() != null ? a.getContentType() : "application/octet-stream");
    }

    // ===================== Xóa =====================

    @Override
    @Transactional
    public AttachmentDeleteResponse delete(Long bookingId, Long attachmentId) {
        BookingAttachment a = getAttachment(bookingId, attachmentId);
        assertOwnerOrAdmin(a.getBooking(), userService.getCurrentUserEntity());

        String name = a.getOriginalFileName();
        fileStorage.deleteAfterCommit(a.getFilePath());
        attachmentRepository.delete(a);
        return new AttachmentDeleteResponse(attachmentId, name, true, "Đã xóa file \"" + name + "\"");
    }

    @Override
    @Transactional
    public void deleteFilesOfBooking(Booking booking) {
        if (booking.getAttachments() != null)
            booking.getAttachments().forEach(a -> fileStorage.deleteAfterCommit(a.getFilePath()));
    }

    // ===================== Helper =====================

    private Booking getBooking(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("lịch đặt", id));
    }

    /** Tìm theo cả id file và id lịch đặt để không tải được file của lịch khác bằng cách đoán id. */
    private BookingAttachment getAttachment(Long bookingId, Long attachmentId) {
        return attachmentRepository.findByIdAndBookingId(attachmentId, bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("file đính kèm", attachmentId));
    }

    private void assertOwnerOrAdmin(Booking b, AppUser current) {
        if (!b.getUser().getId().equals(current.getId()) && !current.hasRole(Role.ADMIN))
            throw new ForbiddenOperationException("Bạn chỉ được thao tác với file của lịch đặt chính mình");
    }

    /** Bỏ phần đường dẫn mà một số trình duyệt gửi kèm tên file. */
    private String sanitizeName(String raw) {
        String name = raw == null ? "" : raw.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        if (name.isEmpty()) name = "file";
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    /** Chuẩn hóa đường dẫn trong thư mục người dùng chọn, chặn ".." và đường dẫn tuyệt đối. */
    private String sanitizeRelativePath(String raw, String fallbackName) {
        String p = (raw == null || raw.isBlank()) ? fallbackName : raw.replace('\\', '/');
        List<String> parts = Arrays.stream(p.split("/"))
                .map(value -> value.trim()).filter(s -> !s.isEmpty() && !s.equals(".")).toList();
        if (parts.isEmpty() || parts.contains(".."))
            throw new BadRequestException("Đường dẫn thư mục không hợp lệ");
        String joined = String.join("/", parts);
        if (joined.length() > 500) throw new BadRequestException("Đường dẫn thư mục quá dài (tối đa 500 ký tự)");
        return joined;
    }
}
