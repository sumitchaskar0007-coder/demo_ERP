package com.jadhavr.erp.fee.repository;
import com.jadhavr.erp.fee.entity.FeeStructure;
import com.jadhavr.erp.fee.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface FeeStructureRepository extends JpaRepository<FeeStructure,Long>,JpaSpecificationExecutor<FeeStructure>{
 Optional<FeeStructure> findFirstByCollegeIdAndDepartmentIdAndAcademicYearInAndStudentCategoryAndStatus(Long c,Long d,Collection<String> years,StudentCategory cat,FeeStructureStatus s);
 Optional<FeeStructure> findFirstByCollegeIdAndDepartmentIdAndAcademicYearInAndStudentCategoryAndCustomCategoryNameIgnoreCaseAndGenderIgnoreCaseAndStatus(Long c,Long d,Collection<String> years,StudentCategory cat,String custom,String gender,FeeStructureStatus s);
 Optional<FeeStructure> findFirstByCollegeIdAndDepartmentIdAndAcademicYearInAndStudentCategoryAndCustomCategoryNameIsNullAndGenderIgnoreCaseAndStatus(Long c,Long d,Collection<String> years,StudentCategory cat,String gender,FeeStructureStatus s);
 boolean existsByCollegeIdAndDepartmentIdAndAcademicYearInAndStudentCategoryAndStatus(Long c,Long d,Collection<String> years,StudentCategory cat,FeeStructureStatus s);
 boolean existsByCollegeIdAndDepartmentIdAndAcademicYearInAndStudentCategoryAndCourseYearIgnoreCaseAndStatus(Long c,Long d,Collection<String> years,StudentCategory cat,String courseYear,FeeStructureStatus s);
 @Query("select distinct f from FeeStructure f where f.college.id=:collegeId and f.department.id=:departmentId and f.status=com.jadhavr.erp.fee.enums.FeeStructureStatus.ACTIVE order by f.studentCategory, f.customCategoryName")
 List<FeeStructure> findPublicCategoryOptions(@Param("collegeId") Long collegeId,@Param("departmentId") Long departmentId);
}
