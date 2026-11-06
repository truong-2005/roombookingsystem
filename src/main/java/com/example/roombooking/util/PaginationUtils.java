package com.example.roombooking.util;

import com.example.roombooking.constant.ApiConstants;
import com.example.roombooking.dto.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.function.Function;

public final class PaginationUtils {

    private PaginationUtils() {}

    /**
     * Tạo Pageable an toàn:
     * - page không âm, size nằm trong [1, MAX_PAGE_SIZE]
     * - sortBy chỉ nhận trường nằm trong danh sách cho phép, sai thì dùng trường mặc định
     */
    public static Pageable build(int page, int size, String sortBy, String sortDir,
                                 Set<String> allowedSortFields, String defaultSortField) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? ApiConstants.DEFAULT_PAGE_SIZE : Math.min(size, ApiConstants.MAX_PAGE_SIZE);

        String field = (sortBy != null && allowedSortFields.contains(sortBy)) ? sortBy : defaultSortField;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
    }

    /** Biến Page<Entity> thành PageResponse<DTO>. */
    public static <E, T> PageResponse<T> toPageResponse(Page<E> page, Function<E, T> mapper) {
        return PageResponse.of(page.map(mapper));
    }

    public static <T> PageResponse<T> toPageResponse(Page<T> page) {
        return PageResponse.of(page);
    }
}