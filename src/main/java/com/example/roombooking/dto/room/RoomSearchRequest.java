package com.example.roombooking.dto.room;

import com.example.roombooking.entity.Room.RoomType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/** Tham số tìm kiếm phòng (query string). */
@Getter
@Setter
public class RoomSearchRequest {

    private String keyword;
    private RoomType type;

    @Min(value = 1, message = "Sức chứa tối thiểu phải lớn hơn 0")
    private Integer minCapacity;

    private Boolean active;

    @Min(value = 0, message = "Số trang không được âm")
    private int page = 0;

    @Min(value = 1, message = "Kích thước trang tối thiểu là 1")
    @Max(value = 100, message = "Kích thước trang tối đa là 100")
    private int size = 10;

    private String sortBy = "code";
    private String sortDir = "asc";
}