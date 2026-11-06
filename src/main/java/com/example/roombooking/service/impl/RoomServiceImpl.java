package com.example.roombooking.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.constant.BookingConstants;
import com.example.roombooking.dto.booking.BookingResponse;
import com.example.roombooking.dto.common.PageResponse;
import com.example.roombooking.dto.room.RoomAvailabilityResponse;
import com.example.roombooking.dto.room.RoomCreateRequest;
import com.example.roombooking.dto.room.RoomDetailResponse;
import com.example.roombooking.dto.room.RoomResponse;
import com.example.roombooking.dto.room.RoomSearchRequest;
import com.example.roombooking.dto.room.RoomStatusRequest;
import com.example.roombooking.dto.room.RoomUpdateRequest;
import com.example.roombooking.entity.Room;
import com.example.roombooking.entity.Room.ImageSource;
import com.example.roombooking.exception.BadRequestException;
import com.example.roombooking.exception.DuplicateResourceException;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.mapper.BookingMapper;
import com.example.roombooking.mapper.RoomMapper;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.RoomRepository;
import com.example.roombooking.service.BookingConflictService;
import com.example.roombooking.service.RoomService;
import com.example.roombooking.specification.RoomSpecification;
import com.example.roombooking.util.DateTimeUtils;
import com.example.roombooking.util.FileStorageUtils;
import com.example.roombooking.util.FileStorageUtils.StoredFile;
import com.example.roombooking.util.PaginationUtils;


@Service
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final RoomMapper roomMapper;
    private final BookingMapper bookingMapper;
    private final BookingConflictService conflictService;
    private final FileStorageUtils fileStorage;

    @java.beans.ConstructorProperties({"roomRepository", "bookingRepository", "roomMapper", "bookingMapper", "conflictService", "fileStorage"})
    public RoomServiceImpl(
        RoomRepository roomRepository,
        BookingRepository bookingRepository,
        RoomMapper roomMapper,
        BookingMapper bookingMapper,
        BookingConflictService conflictService,
        FileStorageUtils fileStorage
    ) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.roomMapper = roomMapper;
        this.bookingMapper = bookingMapper;
        this.conflictService = conflictService;
        this.fileStorage = fileStorage;
    }

    // ===================== Xem =====================

    @Override
    public PageResponse<RoomResponse> search(RoomSearchRequest req) {
        Page<Room> page = roomRepository.findAll(RoomSpecification.search(req), pageable(req));
        return PaginationUtils.toPageResponse(page, roomMapper::toResponse);
    }

    @Override
    public PageResponse<RoomResponse> searchAvailable(RoomSearchRequest req, LocalDateTime start, LocalDateTime end) {
        validateRange(start, end);
        Specification<Room> spec = RoomSpecification.search(req).and(RoomSpecification.freeBetween(start, end));
        Page<Room> page = roomRepository.findAll(spec, pageable(req));
        return PaginationUtils.toPageResponse(page, roomMapper::toResponse);
    }

    @Override
    public List<RoomResponse> findAllActive() {
        return roomRepository.findByActiveTrueOrderByCodeAsc().stream().map(roomMapper::toResponse).toList();
    }

    @Override
    public RoomDetailResponse getById(Long id) {
        return roomMapper.toDetailResponse(getEntity(id));
    }

    @Override
    public RoomAvailabilityResponse checkAvailability(Long roomId, LocalDateTime start, LocalDateTime end) {
        validateRange(start, end);
        Room room = getEntity(roomId);
        List<BookingResponse> conflicts = conflictService.findConflicts(roomId, start, end, null)
                .stream().map(bookingMapper::toResponse).toList();
        return roomMapper.toAvailabilityResponse(room, start, end, conflicts);
    }

    // ===================== Thêm =====================

    @Override
    @Transactional
    public RoomDetailResponse create(RoomCreateRequest req) {
        String code = req.getCode().trim();
        if (roomRepository.existsByCodeIgnoreCase(code))
            throw new DuplicateResourceException("Phòng", "mã", code.toUpperCase());

        Room room = roomMapper.toEntity(req);
        applyImage(room, req.getImageUrl(), req.getImageFile(), false);
        return roomMapper.toDetailResponse(roomRepository.save(room));
    }

    // ===================== Sửa =====================

    @Override
    @Transactional
    public RoomDetailResponse update(Long id, RoomUpdateRequest req) {
        Room room = getEntity(id);
        String code = req.getCode().trim();
        if (roomRepository.existsByCodeIgnoreCaseAndIdNot(code, id))
            throw new DuplicateResourceException("Phòng", "mã", code.toUpperCase());
        if (room.isActive() && !Boolean.TRUE.equals(req.getActive())) assertCanDeactivate(room);

        roomMapper.updateEntity(room, req);
        applyImage(room, req.getImageUrl(), req.getImageFile(), req.isRemoveImage());
        return roomMapper.toDetailResponse(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomDetailResponse updateStatus(Long id, RoomStatusRequest req) {
        Room room = getEntity(id);
        boolean active = Boolean.TRUE.equals(req.active());
        if (room.isActive() && !active) assertCanDeactivate(room);
        room.setActive(active);
        return roomMapper.toDetailResponse(roomRepository.save(room));
    }

    // ===================== Xóa =====================

    @Override
    @Transactional
    public void delete(Long id) {
        Room room = getEntity(id);
        if (bookingRepository.existsByRoomId(id))
            throw new BadRequestException("Phòng đã có lịch đặt nên không thể xóa. Hãy chuyển sang ngừng hoạt động");
        String imagePath = room.getImageSource() == ImageSource.FILE ? room.getImagePath() : null;
        roomRepository.delete(room);
        roomRepository.flush();
        fileStorage.deleteAfterCommit(imagePath);
    }

    // ===================== Dùng nội bộ / helper =====================

    @Override
    public Room getEntity(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("phòng", id));
    }

    private Pageable pageable(RoomSearchRequest req) {
        return PaginationUtils.build(req.getPage(), req.getSize(), req.getSortBy(), req.getSortDir(),
                ApiConstants.ROOM_SORT_FIELDS, "code");
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) throw new BadRequestException("Vui lòng nhập thời gian bắt đầu và kết thúc");
        if (!end.isAfter(start)) throw new BadRequestException("Thời gian kết thúc phải sau thời gian bắt đầu");
    }

    private void assertCanDeactivate(Room room) {
        if (bookingRepository.existsByRoomIdAndStatusInAndEndTimeAfter(
                room.getId(), BookingConstants.VISIBLE_ON_CALENDAR, DateTimeUtils.now()))
            throw new BadRequestException(
                    "Phòng còn lịch chờ duyệt hoặc đã duyệt chưa diễn ra nên không thể ngừng hoạt động");
    }

    /** Ảnh phòng: file thắng link; không gửi gì và không yêu cầu xóa thì giữ nguyên. */
    private void applyImage(Room room, String url, MultipartFile file, boolean remove) {
        boolean hasFile = file != null && !file.isEmpty();
        String trimmedUrl = url != null ? url.trim() : null;
        boolean hasUrl = trimmedUrl != null && !trimmedUrl.isEmpty();
        if (!hasFile && !hasUrl && !remove) return;

        String oldPath = room.getImageSource() == ImageSource.FILE ? room.getImagePath() : null;
        if (hasFile) {
            StoredFile f = fileStorage.saveImage(file, ApiConstants.UPLOAD_ROOM_DIR);
            room.setImageSource(ImageSource.FILE);
            room.setImagePath(f.path());
            room.setImageContentType(f.contentType());
            room.setImageUrl(null);
        } else if (hasUrl) {
            room.setImageSource(ImageSource.LINK);
            room.setImageUrl(trimmedUrl);
            room.setImagePath(null);
            room.setImageContentType(null);
        } else {
            room.setImageSource(ImageSource.NONE);
            room.setImageUrl(null);
            room.setImagePath(null);
            room.setImageContentType(null);
        }
        fileStorage.deleteAfterCommit(oldPath);
    }
}