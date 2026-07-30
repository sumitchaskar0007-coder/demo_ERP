package com.jadhavr.erp.reports.service;

import com.jadhavr.erp.reports.entity.ReportExportJob;
import com.jadhavr.erp.reports.enums.ReportExportType;
import com.jadhavr.erp.security.TestSecurityUsers;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.user.entity.RoleName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportExportScopeServiceTest {
    @Mock private StaffProfileRepository staff;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void principalCannotQueueAnotherCollegesReport() {
        authenticate(RoleName.PRINCIPAL, 10L, 7L);
        ReportExportScopeService service = new ReportExportScopeService(staff);

        assertThrows(
                AccessDeniedException.class,
                () -> service.capture(ReportExportType.STUDENTS, 8L, null));
    }

    @Test
    void hodScopeIsCapturedFromServerSideProfile() {
        authenticate(RoleName.HOD, 10L, 7L);
        StaffProfile profile = mock(StaffProfile.class);
        com.jadhavr.erp.department.entity.Department department =
                mock(com.jadhavr.erp.department.entity.Department.class);
        when(department.getId()).thenReturn(22L);
        when(profile.getDepartment()).thenReturn(department);
        when(staff.findByUserId(10L)).thenReturn(Optional.of(profile));
        ReportExportScopeService service = new ReportExportScopeService(staff);

        var scope = service.capture(ReportExportType.ATTENDANCE, null, null);

        assertEquals(7L, scope.collegeId());
        assertEquals(22L, scope.departmentId());
        assertEquals("HOD", scope.requesterRole());
    }

    @Test
    void jobOwnerStillCannotCrossTenantAfterAccountMoves() {
        authenticate(RoleName.PRINCIPAL, 10L, 8L);
        ReportExportJob job = new ReportExportJob();
        job.setRequesterUserId(10L);
        job.setRequesterCollegeId(7L);
        job.setScopeCollegeId(7L);
        job.setReportType(ReportExportType.STUDENTS);
        ReportExportScopeService service = new ReportExportScopeService(staff);

        assertThrows(AccessDeniedException.class, () -> service.requireCurrentOwner(job));
    }

    private void authenticate(RoleName role, Long userId, Long collegeId) {
        SecurityContextHolder.getContext().setAuthentication(
                TestSecurityUsers.authentication(role, userId, collegeId));
    }
}
