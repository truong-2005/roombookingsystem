package com.example.roombooking.controller;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.dto.role.RoleCreateRequest;
import com.example.roombooking.dto.role.RoleResponse;
import com.example.roombooking.dto.role.RoleUpdateRequest;
import com.example.roombooking.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.ROLES)
@Tag(name = "Roles", description = "Quản lý vai trò (ADMIN)")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(summary = "Danh sách vai trò")
    public List<RoleResponse> findAll() {
        return roleService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết vai trò")
    public RoleResponse getById(@PathVariable Long id) {
        return roleService.getById(id);
    }

    @PostMapping
    @Operation(summary = "Tạo vai trò")
    public RoleResponse create(@Valid @RequestBody RoleCreateRequest request) {
        return roleService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật vai trò")
    public RoleResponse update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return roleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa vai trò")
    public void delete(@PathVariable Long id) {
        roleService.delete(id);
    }
}
