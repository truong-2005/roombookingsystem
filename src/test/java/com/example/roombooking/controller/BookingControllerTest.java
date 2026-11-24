package com.example.roombooking.controller;

import com.example.roombooking.dto.attachment.AttachmentResponse;
import com.example.roombooking.dto.booking.BookingCancelRequest;
import com.example.roombooking.dto.booking.BookingCreateRequest;
import com.example.roombooking.dto.booking.BookingDetailResponse;
import com.example.roombooking.dto.booking.BookingResponse;
import com.example.roombooking.entity.Booking.BookingStatus;
import com.example.roombooking.exception.GlobalExceptionHandler;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.service.BookingConflictService;
import com.example.roombooking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private BookingService bookingService;

    @Mock
    private BookingConflictService conflictService;

    @InjectMocks
    private BookingController bookingController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("GET /api/bookings/{id} trả về HTTP 200 khi tìm thấy lịch đặt")
    void getById_WhenFound_ReturnsStatus200() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        BookingDetailResponse detail = new BookingDetailResponse(
                10L, 1L, "R101", "Phòng 101", 100, 100L, "John Doe", "john@example.com",
                "0900000000", "IT", "Họp Dự Án", "Nội dung họp", 10, start, end,
                BookingStatus.PENDING, null, null, null, null, null,
                List.<AttachmentResponse>of(), LocalDateTime.now(), LocalDateTime.now()
        );

        when(bookingService.getById(10L)).thenReturn(detail);

        mockMvc.perform(get("/api/bookings/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.title").value("Họp Dự Án"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(bookingService, times(1)).getById(10L);
    }

    @Test
    @DisplayName("GET /api/bookings/{id} trả về HTTP 404 khi không tìm thấy lịch")
    void getById_WhenNotFound_ReturnsStatus404() throws Exception {
        when(bookingService.getById(999L)).thenThrow(new ResourceNotFoundException("lịch đặt", 999L));

        mockMvc.perform(get("/api/bookings/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(bookingService, times(1)).getById(999L);
    }

    @Test
    @DisplayName("POST /api/bookings tạo lịch thành công trả về HTTP 200")
    void create_WhenValid_ReturnsStatus200() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        BookingCreateRequest request = new BookingCreateRequest(1L, "Họp Chiến Lược", "Mô tả", 10, start, end);
        BookingResponse response = new BookingResponse(
                15L, 1L, "R101", "Phòng 101", 100L, "John Doe",
                "Họp Chiến Lược", "Mô tả", 10, start, end, BookingStatus.PENDING, null, LocalDateTime.now()
        );

        when(bookingService.create(any(BookingCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15L))
                .andExpect(jsonPath("$.title").value("Họp Chiến Lược"));

        verify(bookingService, times(1)).create(any(BookingCreateRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/bookings/{id}/cancel hủy lịch thành công trả về HTTP 200")
    void cancel_WhenValid_ReturnsStatus200() throws Exception {
        BookingCancelRequest cancelRequest = new BookingCancelRequest("Bận việc đột xuất");
        BookingResponse response = new BookingResponse(
                10L, 1L, "R101", "Phòng 101", 100L, "John Doe",
                "Họp Dự Án", null, 10, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2),
                BookingStatus.CANCELLED, null, LocalDateTime.now()
        );

        when(bookingService.cancel(eq(10L), any(BookingCancelRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/bookings/10/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(bookingService, times(1)).cancel(eq(10L), any(BookingCancelRequest.class));
    }
}
