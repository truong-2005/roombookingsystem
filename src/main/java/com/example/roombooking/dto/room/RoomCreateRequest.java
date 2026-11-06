package com.example.roombooking.dto.room;

import com.example.roombooking.entity.Room.ImageSource;
import com.example.roombooking.entity.Room.RoomType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

/** Tạo phòng. Gửi dạng multipart/form-data. Ảnh: dán link (imageUrl) HOẶC chọn file (imageFile). */
public class RoomCreateRequest {

    @NotBlank(message = "Mã phòng không được để trống")
    @Size(max = 30, message = "Mã phòng tối đa 30 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Mã phòng chỉ gồm chữ, số, dấu chấm, gạch dưới, gạch ngang")
    private String code;

    @NotBlank(message = "Tên phòng không được để trống")
    @Size(max = 100, message = "Tên phòng tối đa 100 ký tự")
    private String name;

    @NotNull(message = "Loại phòng không được để trống")
    private RoomType type;

    @NotNull(message = "Sức chứa không được để trống")
    @Min(value = 1, message = "Sức chứa phải lớn hơn 0")
    @Max(value = 10000, message = "Sức chứa tối đa 10000")
    private Integer capacity;

    @Size(max = 150, message = "Vị trí tối đa 150 ký tự")
    private String location;

    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
    private String description;

    @Size(max = 500, message = "Tiện nghi tối đa 500 ký tự")
    private String amenities;

    @Size(max = 500, message = "Link ảnh tối đa 500 ký tự")
    @Pattern(regexp = "^$|^https?://\\S+$", message = "Link ảnh phải bắt đầu bằng http:// hoặc https://")
    private String imageUrl;

    private MultipartFile imageFile;

    private boolean active = true;

    @JsonIgnore
    @AssertTrue(message = "Chỉ chọn một: dán link ảnh hoặc tải file ảnh lên")
    public boolean isImageSourceValid() {
        boolean hasUrl = imageUrl != null && !imageUrl.isBlank();
        boolean hasFile = imageFile != null && !imageFile.isEmpty();
        return !(hasUrl && hasFile);
    }

    public ImageSource resolveImageSource() {
        if (imageFile != null && !imageFile.isEmpty()) return ImageSource.FILE;
        if (imageUrl != null && !imageUrl.isBlank()) return ImageSource.LINK;
        return ImageSource.NONE;
    }

    // ===== Getters =====
    public String getCode() { return code; }
    public String getName() { return name; }
    public RoomType getType() { return type; }
    public Integer getCapacity() { return capacity; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public String getAmenities() { return amenities; }
    public String getImageUrl() { return imageUrl; }
    public MultipartFile getImageFile() { return imageFile; }
    public boolean isActive() { return active; }

    // ===== Setters =====
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setType(RoomType type) { this.type = type; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public void setLocation(String location) { this.location = location; }
    public void setDescription(String description) { this.description = description; }
    public void setAmenities(String amenities) { this.amenities = amenities; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setImageFile(MultipartFile imageFile) { this.imageFile = imageFile; }
    public void setActive(boolean active) { this.active = active; }
}