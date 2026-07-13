package com.jadhavr.erp.academic.service;

import com.jadhavr.erp.academic.dto.CreateCourseYearRequest;
import com.jadhavr.erp.academic.entity.AcademicClass;
import com.jadhavr.erp.academic.enums.CourseYearName;
import com.jadhavr.erp.academic.mapper.CourseYearMapper;
import com.jadhavr.erp.academic.repository.AcademicClassRepository;
import com.jadhavr.erp.academic.repository.SectionRepository;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.common.exception.DuplicateResourceException;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.department.entity.DepartmentStatus;
import com.jadhavr.erp.department.repository.DepartmentRepository;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseYearServiceImplTest {
    @Mock AcademicClassRepository years; @Mock DepartmentRepository departments; @Mock SectionRepository sections;
    CourseYearServiceImpl service;
    @BeforeEach void setup(){service=new CourseYearServiceImpl(years,departments,new CourseYearMapper(sections));authenticate(1L);}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}

    @Test void principalCreatesCourseYearForOwnDepartment(){Department d=department(2L,1L);when(departments.findById(2L)).thenReturn(Optional.of(d));when(years.save(any())).thenAnswer(i->{AcademicClass c=i.getArgument(0);c.setId(3L);return c;});when(sections.countByAcademicClassId(3L)).thenReturn(0L);var result=service.create(request(2L));assertEquals("BCA-FY",result.code());assertEquals(CourseYearName.FIRST_YEAR,result.yearName());}
    @Test void principalCannotCreateForAnotherCollege(){when(departments.findById(2L)).thenReturn(Optional.of(department(2L,99L)));assertThrows(AccessDeniedException.class,()->service.create(request(2L)));}
    @Test void duplicateCourseYearIsPrevented(){Department d=department(2L,1L);when(departments.findById(2L)).thenReturn(Optional.of(d));when(years.existsByCollegeIdAndDepartmentIdAndAcademicYearAndYearName(1L,2L,"2026-2027",CourseYearName.FIRST_YEAR)).thenReturn(true);assertThrows(DuplicateResourceException.class,()->service.create(request(2L)));}

    private CreateCourseYearRequest request(Long id){return new CreateCourseYearRequest(id,"2026-2027",CourseYearName.FIRST_YEAR,"BCA First Year","bca-fy");}
    private Department department(Long id,Long collegeId){Department d=new Department();d.setId(id);d.setCollege(college(collegeId));d.setName("BCA");d.setCode("BCA");d.setStatus(DepartmentStatus.ACTIVE);return d;}
    private College college(Long id){College c=new College();c.setId(id);c.setName("College");c.setCode("ABC");c.setStatus(CollegeStatus.ACTIVE);return c;}
    private void authenticate(Long collegeId){Role r=new Role();r.setName(RoleName.PRINCIPAL);User u=new User();u.setId(1L);u.setCollege(college(collegeId));u.setFullName("Principal");u.setEmail("p@example.com");u.setPasswordHash("x");u.setStatus(UserStatus.ACTIVE);u.setRoles(Set.of(r));CustomUserDetails d=new CustomUserDetails(u);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(d,null,d.getAuthorities()));}
}
