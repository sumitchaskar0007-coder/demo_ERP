package com.jadhavr.erp.department.mapper;

import com.jadhavr.erp.department.dto.DepartmentResponse;
import com.jadhavr.erp.department.entity.Department;
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
                department.getStatus(),
                department.getCreatedAt(),
                department.getUpdatedAt()
        );
    }
}
