package com.example.roombooking.service;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.example.roombooking.dto.attachment.AttachmentDeleteResponse;
import com.example.roombooking.dto.attachment.AttachmentResponse;
import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.BookingAttachment.UploadSource;

public interface BookingAttachmentService {

    record AttachmentFile(Resource resource, String fileName, String contentType) {}

    /**
     * Tải lên nhiều file. source = FOLDER thì relativePaths là đường dẫn trong thư mục người dùng chọn
     * (khớp thứ tự với files, ví dụ "HoSo/BienBan/a.docx").
     */
    List<AttachmentResponse> upload(Long bookingId, List<MultipartFile> files,
                                    UploadSource source, List<String> relativePaths);

    List<AttachmentResponse> list(Long bookingId);

    AttachmentFile download(Long bookingId, Long attachmentId);

    AttachmentDeleteResponse delete(Long bookingId, Long attachmentId);

    /** Xóa file vật lý của tất cả file đính kèm khi xóa lịch đặt (gọi trước bookingRepository.delete). */
    void deleteFilesOfBooking(Booking booking);
}