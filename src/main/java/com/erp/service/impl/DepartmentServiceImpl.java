package com.erp.service.impl;

import com.erp.dto.request.DepartmentRequest;
import com.erp.dto.response.DepartmentResponse;
import com.erp.dto.response.PageResponse;
import com.erp.entity.Department;
import com.erp.exception.DuplicateResourceException;
import com.erp.exception.InvalidOperationException;
import com.erp.exception.ResourceNotFoundException;
import com.erp.mapper.DepartmentMapper;
import com.erp.repository.DepartmentRepository;
import com.erp.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        if (departmentRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Department", "code", request.getCode());
        }
        if (departmentRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Department", "name", request.getName());
        }

        Department department = Department.builder()
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .headOfDepartmentId(request.getHeadOfDepartmentId())
                .active(true)
                .build();

        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    @Override
    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = findEntity(id);

        departmentRepository.findByCode(request.getCode()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Department", "code", request.getCode());
            }
        });

        department.setName(request.getName());
        department.setCode(request.getCode().toUpperCase());
        department.setDescription(request.getDescription());
        department.setHeadOfDepartmentId(request.getHeadOfDepartmentId());

        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    @Override
    public DepartmentResponse getById(Long id) {
        return departmentMapper.toResponse(findEntity(id));
    }

    @Override
    public PageResponse<DepartmentResponse> getAll(Pageable pageable) {
        Page<DepartmentResponse> page = departmentRepository.findAll(pageable)
                .map(departmentMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    public PageResponse<DepartmentResponse> search(String keyword, Pageable pageable) {
        Page<DepartmentResponse> page = departmentRepository
                .findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(keyword, keyword, pageable)
                .map(departmentMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Department department = findEntity(id);
        department.setActive(false);
        departmentRepository.save(department);
    }

    @Override
    @Transactional
    public void activate(Long id) {
        Department department = findEntity(id);
        department.setActive(true);
        departmentRepository.save(department);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Department department = findEntity(id);
        // Hard delete is only safe while no Student/Course/Faculty reference this
        // department; once those modules exist this should check for dependents
        // and throw InvalidOperationException instead of cascading silently.
        try {
            departmentRepository.delete(department);
        } catch (Exception e) {
            throw new InvalidOperationException(
                    "Department cannot be deleted while it has linked students, faculty, or courses. Deactivate it instead.");
        }
    }

    private Department findEntity(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
    }
}
