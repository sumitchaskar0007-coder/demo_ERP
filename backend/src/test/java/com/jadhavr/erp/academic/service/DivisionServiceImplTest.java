package com.jadhavr.erp.academic.service;

import com.jadhavr.erp.academic.dto.AssignClassTeacherRequest;
import com.jadhavr.erp.academic.dto.CreateDivisionRequest;
import com.jadhavr.erp.academic.dto.UpdateDivisionRequest;
import com.jadhavr.erp.academic.entity.AcademicClass;
import com.jadhavr.erp.academic.entity.Section;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.enums.CourseYearName;
import com.jadhavr.erp.academic.enums.SectionStatus;
import com.jadhavr.erp.academic.mapper.DivisionMapper;
import com.jadhavr.erp.academic.repository.AcademicClassRepository;
import com.jadhavr.erp.academic.repository.SectionRepository;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.DuplicateResourceException;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.department.entity.DepartmentStatus;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.enums.StaffStatus;
import com.jadhavr.erp.staff.enums.StaffType;
import com.jadhavr.erp.staff.mapper.StaffMapper;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.user.entity.*;
import com.jadhavr.erp.user.repository.RoleRepository;
import com.jadhavr.erp.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DivisionServiceImplTest {
    @Mock SectionRepository divisions; @Mock AcademicClassRepository years; @Mock StaffProfileRepository staff;
    @Mock RoleRepository roles; @Mock UserRepository users; DivisionServiceImpl service;
    @BeforeEach void setup(){service=new DivisionServiceImpl(divisions,years,staff,roles,users,new DivisionMapper(),new StaffMapper());authenticate();}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}

    @Test void principalCreatesDivision(){AcademicClass year=year();when(years.findById(3L)).thenReturn(Optional.of(year));when(divisions.save(any())).thenAnswer(i->{Section s=i.getArgument(0);s.setId(4L);return s;});var result=service.create(new CreateDivisionRequest(3L,"Division A","a",60));assertEquals("A",result.code());assertEquals(60,result.capacity());}
    @Test void duplicateDivisionCodePrevented(){when(years.findById(3L)).thenReturn(Optional.of(year()));when(divisions.existsByAcademicClassIdAndAcademicYearAndCodeIgnoreCase(3L,"2026-2027","A")).thenReturn(true);assertThrows(DuplicateResourceException.class,()->service.create(new CreateDivisionRequest(3L,"Division A","A",60)));}
    @Test void assignClassTeacherSucceeds(){Section division=division();StaffProfile teacher=teacher(5L,department(2L));when(divisions.findById(4L)).thenReturn(Optional.of(division));when(staff.findById(5L)).thenReturn(Optional.of(teacher));Role classRole=role(RoleName.CLASS_TEACHER);when(roles.findByName(RoleName.CLASS_TEACHER)).thenReturn(Optional.of(classRole));when(divisions.save(division)).thenReturn(division);var result=service.assignClassTeacher(4L,new AssignClassTeacherRequest(5L));assertEquals("Mr. Kale",result.classTeacherName());}
    @Test void assignmentFailsForAnotherDepartment(){Section division=division();when(divisions.findById(4L)).thenReturn(Optional.of(division));when(staff.findById(5L)).thenReturn(Optional.of(teacher(5L,department(99L))));assertThrows(BadRequestException.class,()->service.assignClassTeacher(4L,new AssignClassTeacherRequest(5L)));verify(divisions,never()).save(any());}
    @Test void assignmentAddsClassTeacherRoleWhenMissing(){Section division=division();StaffProfile teacher=teacher(5L,department(2L));when(divisions.findById(4L)).thenReturn(Optional.of(division));when(staff.findById(5L)).thenReturn(Optional.of(teacher));when(roles.findByName(RoleName.CLASS_TEACHER)).thenReturn(Optional.of(role(RoleName.CLASS_TEACHER)));when(divisions.save(division)).thenReturn(division);service.assignClassTeacher(4L,new AssignClassTeacherRequest(5L));assertTrue(teacher.getUser().getRoles().stream().anyMatch(r->r.getName()==RoleName.CLASS_TEACHER));verify(users).save(teacher.getUser());}
    @Test void editCanMoveDivisionToAnotherDepartmentCourseYear(){Section division=division();division.setClassTeacher(teacher(5L,department(2L)));AcademicClass target=year();target.setId(8L);target.setDepartment(department(9L));target.setName("Commerce First Year");when(divisions.findById(4L)).thenReturn(Optional.of(division));when(years.findById(8L)).thenReturn(Optional.of(target));when(divisions.save(division)).thenReturn(division);var result=service.update(4L,new UpdateDivisionRequest(8L,"Division B","B",50));assertEquals(9L,result.departmentId());assertEquals(8L,result.courseYearId());assertNull(result.classTeacherId());assertEquals("B",result.code());}

    private AcademicClass year(){AcademicClass y=new AcademicClass();y.setId(3L);y.setCollege(college());y.setDepartment(department(2L));y.setAcademicYear("2026-2027");y.setYearName(CourseYearName.FIRST_YEAR);y.setName("BCA First Year");y.setCode("BCA-FY");y.setStatus(AcademicStatus.ACTIVE);return y;}
    private Section division(){Section s=new Section();s.setId(4L);s.setCollege(college());s.setDepartment(department(2L));s.setAcademicClass(year());s.setAcademicYear("2026-2027");s.setName("Division A");s.setCode("A");s.setCapacity(60);s.setStatus(SectionStatus.ACTIVE);return s;}
    private StaffProfile teacher(Long id,Department d){User u=new User();u.setId(8L);u.setCollege(college());u.setFullName("Mr. Kale");u.setEmail("kale@example.com");u.setPasswordHash("x");u.setStatus(UserStatus.ACTIVE);u.setRoles(Set.of(role(RoleName.SUBJECT_TEACHER)));StaffProfile s=new StaffProfile();s.setId(id);s.setUser(u);s.setCollege(college());s.setDepartment(d);s.setEmployeeCode("EMP-1");s.setFullName("Mr. Kale");s.setEmail("kale@example.com");s.setStaffType(StaffType.TEACHER);s.setStatus(StaffStatus.ACTIVE);return s;}
    private Department department(Long id){Department d=new Department();d.setId(id);d.setCollege(college());d.setName("BCA");d.setCode("BCA");d.setStatus(DepartmentStatus.ACTIVE);return d;}
    private College college(){College c=new College();c.setId(1L);c.setName("College");c.setCode("ABC");c.setStatus(CollegeStatus.ACTIVE);return c;}
    private Role role(RoleName n){Role r=new Role();r.setName(n);return r;}
    private void authenticate(){User u=new User();u.setId(1L);u.setCollege(college());u.setFullName("Principal");u.setEmail("p@example.com");u.setPasswordHash("x");u.setStatus(UserStatus.ACTIVE);u.setRoles(Set.of(role(RoleName.PRINCIPAL)));CustomUserDetails d=new CustomUserDetails(u);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(d,null,d.getAuthorities()));}
}
