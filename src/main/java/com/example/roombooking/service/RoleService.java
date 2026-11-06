package com.example.roombooking.service;

import java.util.List;

import com.example.roombooking.dto.role.RoleCreateRequest;
import com.example.roombooking.dto.role.RoleResponse;
import com.example.roombooking.dto.role.RoleUpdateRequest;

public interface RoleService {

    List<RoleResponse> findAll();

    RoleResponse getById(Long id);

    RoleResponse create(RoleCreateRequest req);

    RoleResponse update(Long id, RoleUpdateRequest req);

    void delete(Long id);
}