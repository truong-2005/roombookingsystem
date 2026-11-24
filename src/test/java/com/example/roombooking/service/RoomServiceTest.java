package com.example.roombooking.service;

import com.example.roombooking.dto.room.RoomCreateRequest;
import com.example.roombooking.dto.room.RoomDetailResponse;
import com.example.roombooking.dto.room.RoomResponse;
import com.example.roombooking.entity.Room;
import com.example.roombooking.entity.Room.ImageSource;
import com.example.roombooking.entity.Room.RoomType;
import com.example.roombooking.exception.BadRequestException;
import com.example.roombooking.exception.DuplicateResourceException;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.mapper.BookingMapper;
import com.example.roombooking.mapper.RoomMapper;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.RoomRepository;
import com.example.roombooking.service.impl.RoomServiceImpl;
import com.example.roombooking.util.FileStorageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    @SuppressWarnings("unused")
    private BookingMapper bookingMapper;

    @Mock
    @SuppressWarnings("unused")
    private BookingConflictService conflictService;

    @Mock
    @SuppressWarnings("unused")
    private FileStorageUtils fileStorage;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Room sampleRoom;

    @BeforeEach
    public void setUp() {
        sampleRoom = new Room();
        sampleRoom.setId(1L);
        sampleRoom.setCode("R101");
        sampleRoom.setName("Phòng Hội Thảo Mặt Trời");
        sampleRoom.setType(RoomType.ROOM);
        sampleRoom.setCapacity(20);
        sampleRoom.setLocation("Tầng 3 - Tòa A");
        sampleRoom.setActive(true);
        sampleRoom.setImageSource(ImageSource.NONE);
    }

    @Test
    @DisplayName("getById trả về chi tiết phòng khi ID tồn tại")
    void getById_WhenRoomExists_ReturnsRoomDetail() {
        RoomDetailResponse detailResponse = new RoomDetailResponse(
                1L, "R101", "Phòng Hội Thảo Mặt Trời", RoomType.ROOM, 20,
                "Tầng 3 - Tòa A", "Mô tả", "Máy chiếu", ImageSource.NONE, null, true,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));
        when(roomMapper.toDetailResponse(sampleRoom)).thenReturn(detailResponse);

        RoomDetailResponse result = roomService.getById(1L);

        assertNotNull(result);
        assertEquals("R101", result.code());
        assertEquals("Phòng Hội Thảo Mặt Trời", result.name());
        verify(roomRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getById ném ResourceNotFoundException khi ID phòng không tồn tại")
    void getById_WhenRoomNotFound_ThrowsResourceNotFoundException() {
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> roomService.getById(999L));
        assertNotNull(ex);
        verify(roomRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("create ném DuplicateResourceException khi mã phòng đã tồn tại")
    void create_WhenCodeAlreadyExists_ThrowsDuplicateResourceException() {
        RoomCreateRequest req = new RoomCreateRequest();
        req.setCode("R101");
        req.setName("Phòng Trùng Mã");

        when(roomRepository.existsByCodeIgnoreCase("R101")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> roomService.create(req));
        assertNotNull(ex);
        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("create lưu phòng thành công và trả về chi tiết")
    void create_WhenValid_SavesAndReturnsDetail() {
        RoomCreateRequest req = new RoomCreateRequest();
        req.setCode("R102");
        req.setName("Phòng Họp Mới");
        req.setType(RoomType.ROOM);
        req.setCapacity(15);

        Room newRoom = new Room();
        newRoom.setCode("R102");
        newRoom.setName("Phòng Họp Mới");

        Room savedRoom = new Room();
        savedRoom.setId(2L);
        savedRoom.setCode("R102");
        savedRoom.setName("Phòng Họp Mới");

        RoomDetailResponse detailResponse = new RoomDetailResponse(
                2L, "R102", "Phòng Họp Mới", RoomType.ROOM, 15,
                null, null, null, ImageSource.NONE, null, true,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(roomRepository.existsByCodeIgnoreCase("R102")).thenReturn(false);
        when(roomMapper.toEntity(req)).thenReturn(newRoom);
        when(roomRepository.save(newRoom)).thenReturn(savedRoom);
        when(roomMapper.toDetailResponse(savedRoom)).thenReturn(detailResponse);

        RoomDetailResponse result = roomService.create(req);

        assertNotNull(result);
        assertEquals(2L, result.id());
        assertEquals("R102", result.code());
        verify(roomRepository, times(1)).save(newRoom);
    }

    @Test
    @DisplayName("delete ném BadRequestException khi phòng đã có lịch đặt")
    void delete_WhenRoomHasBookings_ThrowsBadRequestException() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));
        when(bookingRepository.existsByRoomId(1L)).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> roomService.delete(1L));
        assertTrue(ex.getMessage().contains("đã có lịch đặt"));
        verify((CrudRepository<Room, Long>) roomRepository, never()).delete(any(Room.class));
    }

    @Test
    @DisplayName("delete xóa phòng thành công khi phòng chưa có lịch đặt")
    void delete_WhenRoomHasNoBookings_Success() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));
        when(bookingRepository.existsByRoomId(1L)).thenReturn(false);

        assertDoesNotThrow(() -> roomService.delete(1L));

        verify((CrudRepository<Room, Long>) roomRepository, times(1)).delete(sampleRoom);
        verify(roomRepository, times(1)).flush();
    }

    @Test
    @DisplayName("findAllActive trả về danh sách các phòng đang hoạt động")
    void findAllActive_ReturnsActiveRooms() {
        when(roomRepository.findByActiveTrueOrderByCodeAsc()).thenReturn(List.of(sampleRoom));
        RoomResponse response = new RoomResponse(
                1L, "R101", "Phòng Hội Thảo Mặt Trời", RoomType.ROOM, 20,
                "Tầng 3", ImageSource.NONE, null, true
        );
        when(roomMapper.toResponse(sampleRoom)).thenReturn(response);

        List<RoomResponse> result = roomService.findAllActive();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("R101", result.get(0).code());
        verify(roomRepository, times(1)).findByActiveTrueOrderByCodeAsc();
    }
}
