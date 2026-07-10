package com.jadhavr.erp.staff.mapper;

import com.jadhavr.erp.staff.dto.StaffResponse;
import com.jadhavr.erp.staff.entity.StaffProfile;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class StaffMapper {
    public StaffResponse toResponse(StaffProfile profile) {
        return new StaffResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getCollege().getId(),
                profile.getCollege().getName(),
                profile.getCollege().getCode(),
                profile.getEmployeeCode(),
                profile.getFullName(),
                profile.getEmail(),
                profile.getPhone(),
                profile.getStaffType(),
                profile.getStatus(),
                profile.getUser().getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()),
                profile.getJoiningDate(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
