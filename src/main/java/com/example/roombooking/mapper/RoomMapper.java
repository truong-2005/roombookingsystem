package com.example.roombooking.mapper;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.dto.booking.BookingResponse;
import com.example.roombooking.dto.room.RoomAvailabilityResponse;
import com.example.roombooking.dto.room.RoomCreateRequest;
import com.example.roombooking.dto.room.RoomDetailResponse;
import com.example.roombooking.dto.room.RoomResponse;
import com.example.roombooking.dto.room.RoomUpdateRequest;
import com.example.roombooking.entity.Room;
import com.example.roombooking.entity.Room.ImageSource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class RoomMapper {

    // ===================== Entity -> Response =====================

    public RoomResponse toResponse(Room r) {
        if (r == null) return null;
        return new RoomResponse(
                r.getId(), r.getCode(), r.getName(), r.getType(), r.getCapacity(), r.getLocation(),
                r.getImageSource(), resolveImageUrl(r), r.isActive());
    }

    public RoomDetailResponse toDetailResponse(Room r) {
        if (r == null) return null;
        return new RoomDetailResponse(
                r.getId(), r.getCode(), r.getName(), r.getType(), r.getCapacity(), r.getLocation(),
                r.getDescription(), r.getAmenities(),
                r.getImageSource(), resolveImageUrl(r), r.isActive(),
                r.getCreatedAt(), r.getUpdatedAt());
    }

    /** Kết quả kiểm tra phòng trống: conflicts rỗng nghĩa là phòng trống. */
    public RoomAvailabilityResponse toAvailabilityResponse(Room r, LocalDateTime start, LocalDateTime end,
                                                           List<BookingResponse> conflicts) {
        return new RoomAvailabilityResponse(
                r.getId(), r.getCode(), r.getName(), start, end, conflicts.isEmpty(), conflicts);
    }

    // ===================== Request -> Entity =====================

    /**
     * Chưa lưu ảnh file. Nếu có link ảnh thì gán ngay; nếu có imageFile thì service lưu file rồi gọi
     * room.setImageSource(FILE), setImagePath(...), setImageContentType(...).
     */
    public Room toEntity(RoomCreateRequest req) {
        Room room = Room.builder()
                .code(req.getCode().trim().toUpperCase())
                .name(req.getName().trim())
                .type(req.getType())
                .capacity(req.getCapacity())
                .location(blankToNull(req.getLocation()))
                .description(blankToNull(req.getDescription()))
                .amenities(blankToNull(req.getAmenities()))
                .active(req.isActive())
                .build();
        if (req.getImageUrl() != null && !req.getImageUrl().isBlank()) {
            room.setImageSource(ImageSource.LINK);
            room.setImageUrl(req.getImageUrl().trim());
        }
        return room;
    }

    /** Không đụng tới ảnh: service xử lý link, file và removeImage riêng. */
    public void updateEntity(Room room, RoomUpdateRequest req) {
        room.setCode(req.getCode().trim().toUpperCase());
        room.setName(req.getName().trim());
        room.setType(req.getType());
        room.setCapacity(req.getCapacity());
        room.setLocation(blankToNull(req.getLocation()));
        room.setDescription(blankToNull(req.getDescription()));
        room.setAmenities(blankToNull(req.getAmenities()));
        room.setActive(Boolean.TRUE.equals(req.getActive()));
    }

    // ===================== Helper =====================

    public String resolveImageUrl(Room r) {
        if (r == null || r.getImageSource() == null) return null;
        return switch (r.getImageSource()) {
            case LINK -> r.getImageUrl();
            case FILE -> r.getImagePath() == null ? null : ApiConstants.FILES + "/public/" + r.getImagePath();
            case NONE -> null;
        };
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}