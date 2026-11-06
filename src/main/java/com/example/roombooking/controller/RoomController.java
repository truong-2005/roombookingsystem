package com.example.roombooking.controller;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.dto.common.PageResponse;
import com.example.roombooking.dto.room.RoomAvailabilityResponse;
import com.example.roombooking.dto.room.RoomCreateRequest;
import com.example.roombooking.dto.room.RoomDetailResponse;
import com.example.roombooking.dto.room.RoomResponse;
import com.example.roombooking.dto.room.RoomSearchRequest;
import com.example.roombooking.dto.room.RoomStatusRequest;
import com.example.roombooking.dto.room.RoomUpdateRequest;
import com.example.roombooking.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.ROOMS)
@Tag(name = "Rooms", description = "Tìm kiếm phòng họp, trạng thái trống và quản lý phòng")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm phòng", description = "Trả về danh sách phòng có phân trang và bộ lọc.")
    public PageResponse<RoomResponse> search(@Valid @ParameterObject @ModelAttribute RoomSearchRequest request) {
        return roomService.search(request);
    }

    @GetMapping("/active")
    @Operation(summary = "Danh sách phòng đang hoạt động")
    public List<RoomResponse> findAllActive() {
        return roomService.findAllActive();
    }

    @GetMapping("/available")
    @Operation(summary = "Tìm phòng còn trống", description = "Lọc phòng còn trống trong khoảng thời gian yêu cầu.")
    public PageResponse<RoomResponse> searchAvailable(
            @Valid @ParameterObject @ModelAttribute RoomSearchRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return roomService.searchAvailable(request, startTime, endTime);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết phòng")
    public RoomDetailResponse getById(@PathVariable Long id) {
        return roomService.getById(id);
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Kiểm tra tình trạng phòng", description = "Trả danh sách lịch bị trùng; danh sách rỗng nghĩa là phòng còn trống.")
    public RoomAvailabilityResponse checkAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return roomService.checkAvailability(id, startTime, endTime);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tạo phòng", description = "Tạo phòng. Có thể gửi ảnh bằng imageUrl hoặc imageFile.")
    public RoomDetailResponse create(@Valid @ModelAttribute RoomCreateRequest request) {
        return roomService.create(request);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cập nhật phòng", description = "Cập nhật thông tin phòng và ảnh; có thể dùng removeImage để xóa ảnh hiện tại.")
    public RoomDetailResponse update(@PathVariable Long id, @Valid @ModelAttribute RoomUpdateRequest request) {
        return roomService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Thay đổi trạng thái phòng")
    public RoomDetailResponse updateStatus(@PathVariable Long id,
            @Valid @RequestBody RoomStatusRequest request) {
        return roomService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa phòng")
    public void delete(@PathVariable Long id) {
        roomService.delete(id);
    }
}
