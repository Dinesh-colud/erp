package com.erp.service;

import com.erp.dto.request.DepartmentRequest;
import com.erp.dto.response.DepartmentResponse;
import org.springframework.data.domain.Pageable;

import com.erp.dto.response.PageResponse;

public interface DepartmentService {

    DepartmentResponse create(DepartmentRequest request);

    DepartmentResponse update(Long id, DepartmentRequest request);

    DepartmentResponse getById(Long id);

    PageResponse<DepartmentResponse> getAll(Pageable pageable);

    PageResponse<DepartmentResponse> search(String keyword, Pageable pageable);

    void deactivate(Long id);

    void activate(Long id);

    void delete(Long id);
}
