package com.erp.mapper;

import com.erp.dto.response.DepartmentResponse;
import com.erp.entity.Department;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    /**
     * studentCount/facultyCount/courseCount are populated by DepartmentServiceImpl
     * once the Student/Faculty/Course modules (next installments) exist; they
     * default to 0 here so this mapper has no forward dependency on those modules.
     */
    public DepartmentResponse toResponse(Department department) {
        if (department == null) return null;
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .description(department.getDescription())
                .headOfDepartmentId(department.getHeadOfDepartmentId())
                .active(department.isActive())
                .studentCount(0)
                .facultyCount(0)
                .courseCount(0)
                .build();
    }
}
