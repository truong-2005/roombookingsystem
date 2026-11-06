package com.example.roombooking.repository;

import com.example.roombooking.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<Role> findByNameIn(Collection<String> names);

    /** Đếm số người đang giữ vai trò này, để chặn xóa vai trò đang được dùng. */
    @Query("select count(u) from AppUser u join u.roles r where r.id = :roleId")
    long countUsersByRoleId(@Param("roleId") Long roleId);
}