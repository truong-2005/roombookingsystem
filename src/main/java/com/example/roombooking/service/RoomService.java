package com.example.roombooking.service;

import java.time.LocalDateTime;
import java.util.List;

import com.example.roombooking.dto.common.PageResponse;
import com.example.roombooking.dto.room.RoomAvailabilityResponse;
import com.example.roombooking.dto.room.RoomCreateRequest;
import com.example.roombooking.dto.room.RoomDetailResponse;
import com.example.roombooking.dto.room.RoomResponse;
import com.example.roombooking.dto.room.RoomSearchRequest;
import com.example.roombooking.dto.room.RoomStatusRequest;
import com.example.roombooking.dto.room.RoomUpdateRequest;
import com.example.roombooking.entity.Room;

public interface RoomService {

    PageResponse<RoomResponse> search(RoomSearchRequest req);

    /** Tìm phòng còn trống trong khoảng thời gian. */
    PageResponse<RoomResponse> searchAvailable(RoomSearchRequest req, LocalDateTime start, LocalDateTime end);

    /** Danh sách phòng đang hoạt động (đổ vào ô chọn phòng). */
    List<RoomResponse> findAllActive();

    RoomDetailResponse getById(Long id);

    RoomDetailResponse create(RoomCreateRequest req);

    RoomDetailResponse update(Long id, RoomUpdateRequest req);

    void delete(Long id);

    RoomDetailResponse updateStatus(Long id, RoomStatusRequest req);

    RoomAvailabilityResponse checkAvailability(Long roomId, LocalDateTime start, LocalDateTime end);

    Room getEntity(Long id);
}