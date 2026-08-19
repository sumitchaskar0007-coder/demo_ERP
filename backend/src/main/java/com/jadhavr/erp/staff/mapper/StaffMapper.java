package com.jadhavr.erp.staff.mapper;

import com.jadhavr.erp.staff.dto.StaffResponse;
import com.jadhavr.erp.staff.entity.StaffProfile;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.List;

@Component
public class StaffMapper {
    public StaffResponse toResponse(StaffProfile profile) {
        return new StaffResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getCollege().getId(),
                profile.getCollege().getName(),
                profile.getCollege().getCode(),
                profile.getDepartment() == null ? null : profile.getDepartment().getId(),
                profile.getDepartment() == null ? null : profile.getDepartment().getName(),
                profile.getDepartment() == null ? null : profile.getDepartment().getCode(),
                profile.getDepartments().stream().map(item -> item.getId()).sorted().toList(),
                profile.getDepartments().stream().map(item -> item.getName()).sorted().toList(),
                profile.getEmployeeCode(),
                profile.getFullName(),
                profile.getEmail(),
                profile.getPhone(),
                profile.getStaffType(),
                profile.getUser().getRoles().stream()
                        .map(role -> switch (role.getName()) {
                            case PRINCIPAL -> com.jadhavr.erp.staff.enums.StaffType.TEACHER;
                            case HOD -> com.jadhavr.erp.staff.enums.StaffType.HOD;
                            case CLASS_TEACHER -> com.jadhavr.erp.staff.enums.StaffType.CLASS_TEACHER;
                            case SUBJECT_TEACHER -> com.jadhavr.erp.staff.enums.StaffType.SUBJECT_TEACHER;
                            case STUDENT_SECTION -> com.jadhavr.erp.staff.enums.StaffType.STUDENT_SECTION;
                            case FEE_SECTION -> com.jadhavr.erp.staff.enums.StaffType.FEE_SECTION;
                            default -> com.jadhavr.erp.staff.enums.StaffType.GENERAL_STAFF;
                        })
                        .collect(Collectors.toSet()),
                profile.getStatus(),
                profile.getUser().getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()),
                profile.getJoiningDate(),
                List.of(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
