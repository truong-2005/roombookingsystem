package com.example.roombooking.mapper;

import com.example.roombooking.dto.role.RoleCreateRequest;
import com.example.roombooking.dto.role.RoleResponse;
import com.example.roombooking.dto.role.RoleUpdateRequest;
import com.example.roombooking.entity.Role;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleMapper {

    public RoleResponse toResponse(Role role) {
        if (role == null) return null;
        return new RoleResponse(role.getId(), role.getName(), role.getDescription());
    }

    public List<RoleResponse> toResponseList(List<Role> roles) {
        return roles.stream().map(this::toResponse).toList();
    }

    public Role toEntity(RoleCreateRequest req) {
        return Role.builder()
                .name(req.name().trim())
                .description(req.description())
                .build();
    }

    /** Tên vai trò không được đổi, chỉ cập nhật mô tả. */
    public void updateEntity(Role role, RoleUpdateRequest req) {
        role.setDescription(req.description());
    }
}