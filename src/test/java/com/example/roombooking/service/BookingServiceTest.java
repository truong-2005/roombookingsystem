package com.example.roombooking.service;

import com.example.roombooking.constant.BookingConstants;
import com.example.roombooking.dto.booking.*;
import com.example.roombooking.entity.AppUser;
import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.Booking.BookingStatus;
import com.example.roombooking.entity.Role;
import com.example.roombooking.entity.Room;
import com.example.roombooking.exception.BadRequestException;
import com.example.roombooking.exception.ForbiddenOperationException;
import com.example.roombooking.mapper.BookingMapper;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.NotificationRepository;
import com.example.roombooking.repository.RoomRepository;
import com.example.roombooking.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    @SuppressWarnings("unused")
    private NotificationRepository notificationRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private UserService userService;

    @Mock
    private BookingConflictService conflictService;

    @Mock
    @SuppressWarnings("unused")
    private BookingAttachmentService attachmentService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private AppUser ownerUser;
    private AppUser otherUser;
    private Room sampleRoom;
    private Booking sampleBooking;

    @BeforeEach
    public void setUp() {
        Role userRole = new Role(1L, Role.USER, "Người dùng");

        ownerUser = new AppUser();
        ownerUser.setId(10L);
        ownerUser.setUsername("owner");
        ownerUser.setFullName("Nguyen Van Owner");
        ownerUser.setRoles(Set.of(userRole));

        otherUser = new AppUser();
        otherUser.setId(20L);
        otherUser.setUsername("stranger");
        otherUser.setFullName("Tran Van Stranger");
        otherUser.setRoles(Set.of(userRole));

        sampleRoom = new Room();
        sampleRoom.setId(1L);
        sampleRoom.setCode("R101");
        sampleRoom.setName("Phòng 101");
        sampleRoom.setCapacity(20);
        sampleRoom.setActive(true);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        sampleBooking = new Booking();
        sampleBooking.setId(100L);
        sampleBooking.setUser(ownerUser);
        sampleBooking.setRoom(sampleRoom);
        sampleBooking.setTitle("Họp dự án A");
        sampleBooking.setStartTime(start);
        sampleBooking.setEndTime(end);
        sampleBooking.setStatus(BookingStatus.PENDING);
    }

    @Test
    @DisplayName("create lưu booking thành công và gửi thông báo tới quản trị viên")
    void create_WhenValid_SavesBookingAndNotifiesAdmins() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(2);
        BookingCreateRequest req = new BookingCreateRequest(1L, "Họp mới", "Mục đích", 10, start, end);

        Booking entityToSave = new Booking();
        Booking savedBooking = new Booking();
        savedBooking.setId(101L);
        savedBooking.setStartTime(start);
        savedBooking.setEndTime(end);
        savedBooking.setStatus(BookingStatus.PENDING);

        BookingResponse response = new BookingResponse(
                101L, 1L, "R101", "Phòng 101", 10L, "Nguyen Van Owner",
                "Họp mới", "Mục đích", 10, start, end, BookingStatus.PENDING, null, LocalDateTime.now()
        );

        when(userService.getCurrentUserEntity()).thenReturn(ownerUser);
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleRoom));
        doNothing().when(conflictService).validateRules(sampleRoom, start, end, 10);
        doNothing().when(conflictService).assertNoConflict(1L, start, end, null);
        when(bookingMapper.toEntity(req, sampleRoom, ownerUser)).thenReturn(entityToSave);
        when(bookingRepository.save(entityToSave)).thenReturn(savedBooking);
        when(bookingMapper.toResponse(savedBooking)).thenReturn(response);

        BookingResponse result = bookingService.create(req);

        assertNotNull(result);
        assertEquals(101L, result.id());
        assertEquals(BookingStatus.PENDING, result.status());

        verify(conflictService, times(1)).validateRules(sampleRoom, start, end, 10);
        verify(conflictService, times(1)).assertNoConflict(1L, start, end, null);
        verify(bookingRepository, times(1)).save(entityToSave);
        verify(notificationService, times(1)).notifyAdmins(any(), any(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("getById trả về chi tiết lịch khi người xem chính là chủ lịch")
    void getById_WhenUserIsOwner_ReturnsDetailResponse() {
        BookingDetailResponse detailResponse = new BookingDetailResponse(
                100L, 1L, "R101", "Phòng 101", 20, 10L, "Nguyen Van Owner", "owner@example.com",
                "0987654321", "IT", "Họp dự án A", null, 10, sampleBooking.getStartTime(), sampleBooking.getEndTime(),
                BookingStatus.PENDING, null, null, null, null, null,
                Collections.emptyList(), LocalDateTime.now(), LocalDateTime.now()
        );

        when(bookingRepository.findWithDetailsById(100L)).thenReturn(Optional.of(sampleBooking));
        when(userService.getCurrentUserEntity()).thenReturn(ownerUser);
        when(bookingMapper.toDetailResponse(sampleBooking)).thenReturn(detailResponse);

        BookingDetailResponse result = bookingService.getById(100L);

        assertNotNull(result);
        assertEquals(100L, result.id());
        assertEquals("Họp dự án A", result.title());
    }

    @Test
    @DisplayName("getById ném ForbiddenOperationException khi người xem không phải chủ lịch và không phải Admin")
    void getById_WhenUserNotOwnerAndNotAdmin_ThrowsForbiddenOperationException() {
        when(bookingRepository.findWithDetailsById(100L)).thenReturn(Optional.of(sampleBooking));
        when(userService.getCurrentUserEntity()).thenReturn(otherUser);

        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class,
                () -> bookingService.getById(100L));
        assertNotNull(ex);
    }

    @Test
    @DisplayName("cancel hủy lịch thành công khi lịch đang ở trạng thái PENDING")
    void cancel_WhenBookingPending_CancelsSuccessfully() {
        BookingCancelRequest cancelReq = new BookingCancelRequest("Bận việc đột xuất");

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(sampleBooking));
        when(userService.getCurrentUserEntity()).thenReturn(ownerUser);

        Booking cancelledBooking = new Booking();
        cancelledBooking.setId(100L);
        cancelledBooking.setUser(ownerUser);
        cancelledBooking.setRoom(sampleRoom);
        cancelledBooking.setTitle("Họp dự án A");
        cancelledBooking.setStartTime(sampleBooking.getStartTime());
        cancelledBooking.setEndTime(sampleBooking.getEndTime());
        cancelledBooking.setStatus(BookingStatus.CANCELLED);
        cancelledBooking.setCancelReason("Bận việc đột xuất");
        cancelledBooking.setCancelledAt(LocalDateTime.now());

        BookingResponse response = new BookingResponse(
                100L, 1L, "R101", "Phòng 101", 10L, "owner",
                "Họp dự án A", null, 10, sampleBooking.getStartTime(), sampleBooking.getEndTime(),
                BookingStatus.CANCELLED, null, LocalDateTime.now()
        );

        when(bookingRepository.save(sampleBooking)).thenReturn(cancelledBooking);
        when(bookingMapper.toResponse(cancelledBooking)).thenReturn(response);

        BookingResponse result = bookingService.cancel(100L, cancelReq);

        assertNotNull(result);
        assertEquals(BookingStatus.CANCELLED, result.status());
        verify(bookingRepository, times(1)).save(sampleBooking);
    }

    @Test
    @DisplayName("cancel ném BadRequestException khi lịch đã bị từ chối hoặc hủy từ trước")
    void cancel_WhenAlreadyRejected_ThrowsBadRequestException() {
        sampleBooking.setStatus(BookingStatus.REJECTED);
        BookingCancelRequest cancelReq = new BookingCancelRequest("Muốn hủy tiếp");

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(sampleBooking));
        when(userService.getCurrentUserEntity()).thenReturn(ownerUser);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> bookingService.cancel(100L, cancelReq));

        assertEquals(BookingConstants.ERR_NOT_CANCELLABLE, ex.getMessage());
        verify(bookingRepository, never()).save(any());
    }
}
