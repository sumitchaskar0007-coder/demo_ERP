package com.collegeerp.erp.department.mapper;

import com.collegeerp.erp.department.dto.DepartmentResponse;
import com.collegeerp.erp.department.entity.Department;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getCollege().getId(),
                department.getCollege().getName(),
                department.getCollege().getCode(),
                department.getName(),
                department.getCode(),
                department.getDescription(),
                department.getAdmissionFormFee(),
                department.getStatus(),
                department.getCreatedAt(),
                department.getUpdatedAt()
        );
    }
}
