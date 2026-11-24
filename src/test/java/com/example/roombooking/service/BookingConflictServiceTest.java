package com.example.roombooking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.roombooking.constant.BookingConstants;
import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.Room;
import com.example.roombooking.exception.BadRequestException;
import com.example.roombooking.exception.BookingConflictException;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.mapper.BookingMapper;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.RoomRepository;
import com.example.roombooking.service.impl.BookingConflictServiceImpl;

@ExtendWith(MockitoExtension.class)
class BookingConflictServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    @SuppressWarnings("unused")
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingConflictServiceImpl conflictService;

    private Room activeRoom;

    @BeforeEach
    public void setUp() {
        activeRoom = new Room();
        activeRoom.setId(1L);
        activeRoom.setCode("R101");
        activeRoom.setName("Phòng Họp 1");
        activeRoom.setCapacity(20);
        activeRoom.setActive(true);
    }

    // ==========================================
    // validateRules Tests
    // ==========================================

    @Test
    @DisplayName("Ném BadRequestException khi phòng đang ngừng hoạt động")
    void validateRules_WhenRoomInactive_ThrowsBadRequestException() {
        activeRoom.setActive(false);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime start = LocalDateTime.of(tomorrow, LocalTime.of(9, 0));
        LocalDateTime end = LocalDateTime.of(tomorrow, LocalTime.of(10, 0));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> conflictService.validateRules(activeRoom, start, end, 10));
        assertEquals(BookingConstants.ERR_ROOM_INACTIVE, ex.getMessage());
    }

    @Test
    @DisplayName("Ném BadRequestException khi thời gian kết thúc trước hoặc bằng thời gian bắt đầu")
    void validateRules_WhenEndNotAfterStart_ThrowsBadRequestException() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime start = LocalDateTime.of(tomorrow, LocalTime.of(10, 0));
        LocalDateTime end = LocalDateTime.of(tomorrow, LocalTime.of(9, 0));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> conflictService.validateRules(activeRoom, start, end, 5));
        assertTrue(ex.getMessage().contains("Thời gian kết thúc phải sau"));
    }

    @Test
    @DisplayName("Ném BadRequestException khi đặt phòng vắt qua 2 ngày khác nhau")
    void validateRules_WhenSpanningDifferentDays_ThrowsBadRequestException() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime start = LocalDateTime.of(tomorrow, LocalTime.of(21, 0));
        LocalDateTime end = LocalDateTime.of(tomorrow.plusDays(1), LocalTime.of(7, 0));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> conflictService.validateRules(activeRoom, start, end, 5));
        assertTrue(ex.getMessage().contains("trong cùng một ngày"));
    }

    @Test
    @DisplayName("Ném BadRequestException khi đặt phòng trong quá khứ")
    void validateRules_WhenInThePast_ThrowsBadRequestException() {
        LocalDateTime pastStart = LocalDateTime.now().minusDays(1).withHour(9).withMinute(0);
        LocalDateTime pastEnd = pastStart.plusHours(1);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> conflictService.validateRules(activeRoom, pastStart, pastEnd, 5));
        assertTrue(ex.getMessage().contains("quá khứ"));
    }

    @Test
    @DisplayName("Ném BadRequestException khi đặt ngoài khung giờ mở cửa (trước 06:00 hoặc sau 22:00)")
    void validateRules_WhenOutsideOperatingHours_ThrowsBadRequestException() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime earlyStart = LocalDateTime.of(tomorrow, LocalTime.of(5, 30));
        LocalDateTime earlyEnd = LocalDateTime.of(tomorrow, LocalTime.of(7, 0));

        BadRequestException ex1 = assertThrows(BadRequestException.class,
                () -> conflictService.validateRules(activeRoom, earlyStart, earlyEnd, 5));
        assertNotNull(ex1);

        LocalDateTime lateStart = LocalDateTime.of(tomorrow, LocalTime.of(21, 0));
        LocalDateTime lateEnd = LocalDateTime.of(tomorrow, LocalTime.of(22, 30));

        BadRequestException ex2 = assertThrows(BadRequestException.class,
                () -> conflictService.validateRules(activeRoom, lateStart, lateEnd, 5));
        assertNotNull(ex2);
    }

    @Test
    @DisplayName("Ném BadRequestException khi thời lượng cuộc họp dưới 30 phút")
    void validateRules_WhenDurationTooShort_ThrowsBadRequestException() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime start = LocalDateTime.of(tomorrow, LocalTime.of(9, 0));
        LocalDateTime end = LocalDateTime.of(tomorrow, LocalTime.of(9, 20)); // 20 phút

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> conflictService.validateRules(activeRoom, start, end, 5));
        assertTrue(ex.getMessage().contains("tối thiểu là 30 phút"));
    }

    @Test
    @DisplayName("Ném BadRequestException khi số người tham dự vượt quá sức chứa của phòng")
    void validateRules_WhenAttendeesExceedCapacity_ThrowsBadRequestException() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime start = LocalDateTime.of(tomorrow, LocalTime.of(9, 0));
        LocalDateTime end = LocalDateTime.of(tomorrow, LocalTime.of(10, 0));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> conflictService.validateRules(activeRoom, start, end, 30)); // sức chứa là 20
        assertTrue(ex.getMessage().contains("vượt quá sức chứa"));
    }

    @Test
    @DisplayName("Không ném lỗi khi thông tin đặt phòng hoàn toàn hợp lệ")
    void validateRules_WhenValid_Success() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime start = LocalDateTime.of(tomorrow, LocalTime.of(9, 0));
        LocalDateTime end = LocalDateTime.of(tomorrow, LocalTime.of(11, 0));

        assertDoesNotThrow(() -> conflictService.validateRules(activeRoom, start, end, 15));
    }

    // ==========================================
    // findConflicts & assertNoConflict Tests
    // ==========================================

    @Test
    @DisplayName("findConflicts gọi đúng Repository với danh sách trạng thái chặn")
    void findConflicts_CallsRepositoryWithBlockingStatuses() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        when(bookingRepository.findConflicts(eq(1L), eq(BookingConstants.BLOCKING_STATUSES), eq(start), eq(end), eq(10L)))
                .thenReturn(Collections.emptyList());

        List<Booking> result = conflictService.findConflicts(1L, start, end, 10L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(bookingRepository, times(1))
                .findConflicts(1L, BookingConstants.BLOCKING_STATUSES, start, end, 10L);
    }

    @Test
    @DisplayName("assertNoConflict ném BookingConflictException khi có lịch trùng")
    void assertNoConflict_WhenConflictExists_ThrowsBookingConflictException() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        Booking existing = new Booking();
        existing.setId(99L);
        existing.setTitle("Họp dự án cũ");
        existing.setStartTime(start);
        existing.setEndTime(end);

        when(bookingRepository.findConflicts(eq(1L), eq(BookingConstants.BLOCKING_STATUSES), eq(start), eq(end), eq(-1L)))
                .thenReturn(List.of(existing));

        BookingConflictException ex = assertThrows(BookingConflictException.class,
                () -> conflictService.assertNoConflict(1L, start, end, null));

        assertTrue(ex.getMessage().contains("Họp dự án cũ"));
    }

    @Test
    @DisplayName("assertNoConflict thành công khi không có lịch nào bị trùng")
    void assertNoConflict_WhenNoConflict_Success() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        when(bookingRepository.findConflicts(anyLong(), anyList(), any(), any(), anyLong()))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> conflictService.assertNoConflict(1L, start, end, null));
    }

    // ==========================================
    // check Tests
    // ==========================================

    @Test
    @DisplayName("check ném ResourceNotFoundException khi phòng không tồn tại")
    void check_WhenRoomNotFound_ThrowsResourceNotFoundException() {
        when(roomRepository.existsById(999L)).thenReturn(false);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        ResourceNotFoundException ex3 = assertThrows(ResourceNotFoundException.class,
                () -> conflictService.check(999L, start, end, null));
        assertNotNull(ex3);
    }

    @Test
    @DisplayName("check ném BadRequestException khi thời gian bắt đầu hoặc kết thúc bị null")
    void check_WhenStartOrEndNull_ThrowsBadRequestException() {
        when(roomRepository.existsById(1L)).thenReturn(true);

        BadRequestException ex4 = assertThrows(BadRequestException.class,
                () -> conflictService.check(1L, null, LocalDateTime.now(), null));
        assertNotNull(ex4);
    }
}
