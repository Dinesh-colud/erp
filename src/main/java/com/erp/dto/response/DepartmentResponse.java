package com.erp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Long headOfDepartmentId;
    private String headOfDepartmentName;
    private boolean active;
    private long studentCount;
    private long facultyCount;
    private long courseCount;
}
