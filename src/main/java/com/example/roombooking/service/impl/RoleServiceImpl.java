package com.example.roombooking.service.impl;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.roombooking.dto.role.RoleCreateRequest;
import com.example.roombooking.dto.role.RoleResponse;
import com.example.roombooking.dto.role.RoleUpdateRequest;
import com.example.roombooking.entity.Role;
import com.example.roombooking.exception.BadRequestException;
import com.example.roombooking.exception.DuplicateResourceException;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.mapper.RoleMapper;
import com.example.roombooking.repository.RoleRepository;
import com.example.roombooking.service.RoleService;


@Service
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    /** Vai trò hệ thống, không được xóa. */
    private static final Set<String> PROTECTED_ROLES = Set.of(Role.ADMIN, Role.USER);

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @java.beans.ConstructorProperties({"roleRepository", "roleMapper"})
    public RoleServiceImpl(
        RoleRepository roleRepository,
        RoleMapper roleMapper
    ) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    @Override
    public List<RoleResponse> findAll() {
        return roleMapper.toResponseList(roleRepository.findAll());
    }

    @Override
    public RoleResponse getById(Long id) {
        return roleMapper.toResponse(getEntity(id));
    }

    @Override
    @Transactional
    public RoleResponse create(RoleCreateRequest req) {
        String name = req.name().trim();
        if (roleRepository.existsByName(name))
            throw new DuplicateResourceException("Vai trò", "tên", name);
        return roleMapper.toResponse(roleRepository.save(roleMapper.toEntity(req)));
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RoleUpdateRequest req) {
        Role role = getEntity(id);
        roleMapper.updateEntity(role, req);
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = getEntity(id);
        if (PROTECTED_ROLES.contains(role.getName()))
            throw new BadRequestException("Không thể xóa vai trò hệ thống " + role.getName());
        long users = roleRepository.countUsersByRoleId(id);
        if (users > 0)
            throw new BadRequestException("Vai trò đang được gán cho " + users + " người dùng nên không thể xóa");
        roleRepository.delete(role);
    }

    private Role getEntity(Long id) {
        return roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("vai trò", id));
    }
}