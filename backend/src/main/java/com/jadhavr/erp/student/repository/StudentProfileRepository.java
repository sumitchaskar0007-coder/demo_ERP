package com.jadhavr.erp.student.repository;

import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.fee.dto.CollegeCountPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long>,
        JpaSpecificationExecutor<StudentProfile> {
    @EntityGraph(attributePaths = {"college", "department"})
    Optional<StudentProfile> findByUserId(Long userId);
    Optional<StudentProfile> findByAdmissionNumber(String admissionNumber);
    boolean existsByAdmissionNumber(String admissionNumber);
    boolean existsByRollNumber(String rollNumber);
    boolean existsByEmailAndCollegeId(String email, Long collegeId);
    List<StudentProfile> findByCollegeId(Long collegeId);
    List<StudentProfile> findByDepartmentId(Long departmentId);
    long countByCollegeId(Long collegeId);
    long countByDepartmentId(Long departmentId);

    @Query("""
            select new com.jadhavr.erp.fee.dto.CollegeCountPoint(s.college.name, count(s.id))
            from StudentProfile s
            where s.status = com.jadhavr.erp.student.enums.StudentStatus.ACTIVE
            group by s.college.id, s.college.name
            order by s.college.name
            """)
    List<CollegeCountPoint> countStudentsByCollege();
}
