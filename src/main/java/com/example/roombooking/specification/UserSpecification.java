package com.example.roombooking.specification;

import com.example.roombooking.entity.AppUser;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class UserSpecification {

    private UserSpecification() {}

    /**
     * Tìm người dùng (ADMIN dùng).
     *
     * @param keyword  tìm theo username, email, họ tên, số điện thoại, đơn vị
     * @param enabled  lọc theo trạng thái hoạt động / bị khóa (null = tất cả)
     * @param roleName lọc theo tên vai trò, ví dụ ROLE_ADMIN (null = tất cả)
     */
    public static Specification<AppUser> search(String keyword, Boolean enabled, String roleName) {
        List<Specification<AppUser>> specs = new ArrayList<>();
        specs.add(keyword(keyword));
        specs.add(isEnabled(enabled));
        specs.add(hasRole(roleName));
        return Specification.allOf(specs);
    }

    public static Specification<AppUser> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String pattern = "%" + escape(keyword.trim().toLowerCase()) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("username")), pattern, '\\'),
                    cb.like(cb.lower(root.get("email")), pattern, '\\'),
                    cb.like(cb.lower(root.get("fullName")), pattern, '\\'),
                    cb.like(cb.lower(root.get("phone")), pattern, '\\'),
                    cb.like(cb.lower(root.get("department")), pattern, '\\'));
        };
    }

    public static Specification<AppUser> isEnabled(Boolean enabled) {
        return (root, query, cb) -> enabled == null ? cb.conjunction() : cb.equal(root.get("enabled"), enabled);
    }

    public static Specification<AppUser> hasRole(String roleName) {
        return (root, query, cb) -> {
            if (roleName == null || roleName.isBlank()) return cb.conjunction();
            Join<AppUser, ?> roles = root.join("roles", JoinType.INNER);
            return cb.equal(roles.get("name"), roleName.trim());
        };
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}