package com.example.roombooking.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.roombooking.dto.room.RoomDetailResponse;
import com.example.roombooking.dto.room.RoomResponse;
import com.example.roombooking.entity.Room.ImageSource;
import com.example.roombooking.entity.Room.RoomType;
import com.example.roombooking.exception.GlobalExceptionHandler;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.service.RoomService;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private RoomController roomController;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roomController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }



    @Test
    @DisplayName("GET /api/rooms/active trả về HTTP 200 và danh sách phòng đang hoạt động")
    void findAllActive_ReturnsStatus200AndList() throws Exception {
        RoomResponse roomResponse = new RoomResponse(
                1L, "R101", "Phòng Hội Thảo 1", RoomType.ROOM, 20,
                "Tầng 3", ImageSource.NONE, null, true
        );

        when(roomService.findAllActive()).thenReturn(List.of(roomResponse));

        mockMvc.perform(get("/api/rooms/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].code").value("R101"))
                .andExpect(jsonPath("$[0].name").value("Phòng Hội Thảo 1"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(roomService, times(1)).findAllActive();
    }

    @Test
    @DisplayName("GET /api/rooms/{id} trả về HTTP 200 khi phòng tồn tại")
    void getById_WhenRoomExists_ReturnsStatus200() throws Exception {
        RoomDetailResponse detailResponse = new RoomDetailResponse(
                1L, "R101", "Phòng Hội Thảo 1", RoomType.ROOM, 20,
                "Tầng 3", "Mô tả chi tiết", "Máy chiếu", ImageSource.NONE, null, true,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(roomService.getById(1L)).thenReturn(detailResponse);

        mockMvc.perform(get("/api/rooms/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.code").value("R101"))
                .andExpect(jsonPath("$.capacity").value(20));

        verify(roomService, times(1)).getById(1L);
    }

    @Test
    @DisplayName("GET /api/rooms/{id} trả về HTTP 404 khi phòng không tồn tại")
    void getById_WhenNotFound_ReturnsStatus404() throws Exception {
        when(roomService.getById(999L)).thenThrow(new ResourceNotFoundException("phòng", 999L));

        mockMvc.perform(get("/api/rooms/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(roomService, times(1)).getById(999L);
    }

    @Test
    @DisplayName("DELETE /api/rooms/{id} trả về HTTP 204 No Content khi xóa thành công")
    void delete_WhenSuccess_ReturnsStatus204() throws Exception {
        doNothing().when(roomService).delete(1L);

        mockMvc.perform(delete("/api/rooms/1"))
                .andExpect(status().isNoContent());

        verify(roomService, times(1)).delete(1L);
    }
}
