package com.jadhavr.erp.reports.service;

import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.reports.entity.ReportExportJob;
import com.jadhavr.erp.reports.enums.ReportExportType;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

@Service
public class ReportExportScopeService {
    private final StaffProfileRepository staff;

    public ReportExportScopeService(StaffProfileRepository staff) {
        this.staff = staff;
    }

    public CapturedScope capture(
            ReportExportType type, Long requestedCollegeId, Long requestedDepartmentId) {
        authorize(type);
        CustomUserDetails current = SecurityUtils.requireCurrentUser();
        Long ownCollegeId = current.getCollegeId();
        Long collegeId;
        Long departmentId = requestedDepartmentId;

        if (SecurityUtils.isSuperAdmin()) {
            if (requestedCollegeId == null) {
                throw new BadRequestException(
                        "collegeId is required for an asynchronous report export");
            }
            collegeId = requestedCollegeId;
        } else {
            if (ownCollegeId == null) throw new AccessDeniedException("College access is required");
            if (requestedCollegeId != null && !Objects.equals(requestedCollegeId, ownCollegeId)) {
                throw new AccessDeniedException("Report is outside your college");
            }
            collegeId = ownCollegeId;
        }

        if (SecurityUtils.hasRole("HOD")) {
            StaffProfile profile = staff.findByUserId(current.getId())
                    .orElseThrow(() -> new AccessDeniedException("HOD profile is required"));
            Long ownDepartmentId = profile.getDepartment() == null
                    ? null : profile.getDepartment().getId();
            if (ownDepartmentId == null) {
                throw new AccessDeniedException("HOD department is required");
            }
            if (requestedDepartmentId != null
                    && !profile.belongsToDepartment(requestedDepartmentId)) {
                throw new AccessDeniedException("Report is outside your department");
            }
            departmentId = requestedDepartmentId == null
                    ? ownDepartmentId : requestedDepartmentId;
        }

        return new CapturedScope(
                current.getId(),
                ownCollegeId,
                collegeId,
                departmentId,
                effectiveRole(type));
    }

    public void requireCurrentOwner(ReportExportJob job) {
        CustomUserDetails current = SecurityUtils.requireCurrentUser();
        if (!Objects.equals(job.getRequesterUserId(), current.getId())) {
            throw new AccessDeniedException("Report export belongs to another user");
        }
        authorize(job.getReportType());
        if (job.getRequesterCollegeId() == null) {
            if (!SecurityUtils.isSuperAdmin()) {
                throw new AccessDeniedException("Report export access has changed");
            }
        } else if (!Objects.equals(job.getRequesterCollegeId(), current.getCollegeId())) {
            throw new AccessDeniedException("Report export is outside your college");
        }
        if (SecurityUtils.hasRole("HOD")) {
            StaffProfile profile = staff.findByUserId(current.getId())
                    .orElseThrow(() -> new AccessDeniedException("HOD profile is required"));
            if (job.getScopeDepartmentId() == null
                    || !profile.belongsToDepartment(job.getScopeDepartmentId())) {
                throw new AccessDeniedException("Report export is outside your department");
            }
        }
    }

    public void authorize(ReportExportType type) {
        boolean allowed = SecurityUtils.isSuperAdmin()
                || SecurityUtils.hasRole("PRINCIPAL")
                || (type == ReportExportType.FEES && SecurityUtils.hasRole("FEE_SECTION"))
                || (Set.of(ReportExportType.ADMISSIONS, ReportExportType.STUDENTS).contains(type)
                        && SecurityUtils.hasRole("STUDENT_SECTION"))
                || (Set.of(
                                ReportExportType.ADMISSIONS,
                                ReportExportType.STUDENTS,
                                ReportExportType.ATTENDANCE)
                        .contains(type)
                        && SecurityUtils.hasRole("HOD"));
        if (!allowed) throw new AccessDeniedException("You are not allowed to export this report");
    }

    private String effectiveRole(ReportExportType type) {
        if (SecurityUtils.isSuperAdmin()) return "SUPER_ADMIN";
        if (SecurityUtils.hasRole("PRINCIPAL")) return "PRINCIPAL";
        if (type == ReportExportType.FEES && SecurityUtils.hasRole("FEE_SECTION")) {
            return "FEE_SECTION";
        }
        if (SecurityUtils.hasRole("HOD")) return "HOD";
        if (SecurityUtils.hasRole("STUDENT_SECTION")) return "STUDENT_SECTION";
        throw new AccessDeniedException("You are not allowed to export this report");
    }

    public record CapturedScope(
            Long requesterUserId,
            Long requesterCollegeId,
            Long collegeId,
            Long departmentId,
            String requesterRole) {}
}
