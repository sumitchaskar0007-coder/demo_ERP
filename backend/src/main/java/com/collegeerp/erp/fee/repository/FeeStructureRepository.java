package com.collegeerp.erp.fee.repository;
import com.collegeerp.erp.fee.entity.FeeStructure;
import com.collegeerp.erp.fee.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface FeeStructureRepository extends JpaRepository<FeeStructure,Long>,JpaSpecificationExecutor<FeeStructure>{
 Optional<FeeStructure> findFirstByCollegeIdAndDepartmentIdAndAcademicYearInAndStudentCategoryAndStatus(Long c,Long d,Collection<String> years,StudentCategory cat,FeeStructureStatus s);
 Optional<FeeStructure> findFirstByCollegeIdAndDepartmentIdAndAcademicYearInAndStudentCategoryAndCustomCategoryNameIgnoreCaseAndGenderIgnoreCaseAndStatus(Long c,Long d,Collection<String> years,StudentCategory cat,String custom,String gender,FeeStructureStatus s);
 Optional<FeeStructure> findFirstByCollegeIdAndDepartmentIdAndAcademicYearInAndStudentCategoryAndCustomCategoryNameIsNullAndGenderIgnoreCaseAndStatus(Long c,Long d,Collection<String> years,StudentCategory cat,String gender,FeeStructureStatus s);
 Optional<FeeStructure> findFirstByCollegeIdAndDepartmentIdAndAcademicYearInAndStudentCategoryAndCustomCategoryNameIgnoreCaseAndGenderIgnoreCaseAndCourseYearIgnoreCaseAndStatus(Long c,Long d,Collection<String> years,StudentCategory cat,String custom,String gender,String courseYear,FeeStructureStatus s);
 Optional<FeeStructure> findFirstByCollegeIdAndDepartmentIdAndAcademicYearInAndStudentCategoryAndCustomCategoryNameIsNullAndGenderIgnoreCaseAndCourseYearIgnoreCaseAndStatus(Long c,Long d,Collection<String> years,StudentCategory cat,String gender,String courseYear,FeeStructureStatus s);
 @Query("""
   select f from FeeStructure f
   where f.college.id = :collegeId
     and f.department.id = :departmentId
     and f.academicYear in :academicYears
     and f.studentCategory = :category
     and f.customCategoryName is null
     and lower(f.gender) = lower(:gender)
     and lower(f.courseYear) = lower(:courseYear)
     and f.status = :status
   order by f.id
   """)
 List<FeeStructure> findAllConfiguredAssessmentsWithoutCustomCategory(
   @Param("collegeId") Long collegeId,
   @Param("departmentId") Long departmentId,
   @Param("academicYears") Collection<String> academicYears,
   @Param("category") StudentCategory category,
   @Param("gender") String gender,
   @Param("courseYear") String courseYear,
   @Param("status") FeeStructureStatus status);

 @Query("""
   select f from FeeStructure f
   where f.college.id = :collegeId
     and f.department.id = :departmentId
     and f.academicYear in :academicYears
     and f.studentCategory = :category
     and lower(f.customCategoryName) = lower(:customCategoryName)
     and lower(f.gender) = lower(:gender)
     and lower(f.courseYear) = lower(:courseYear)
     and f.status = :status
   order by f.id
   """)
 List<FeeStructure> findAllConfiguredAssessmentsWithCustomCategory(
   @Param("collegeId") Long collegeId,
   @Param("departmentId") Long departmentId,
   @Param("academicYears") Collection<String> academicYears,
   @Param("category") StudentCategory category,
   @Param("customCategoryName") String customCategoryName,
   @Param("gender") String gender,
   @Param("courseYear") String courseYear,
   @Param("status") FeeStructureStatus status);

 @Query("""
   select f from FeeStructure f
   where f.college.id = :collegeId
     and f.department.id = :departmentId
     and f.academicYear in :academicYears
     and lower(f.gender) = lower(:gender)
     and lower(f.courseYear) = lower(:courseYear)
     and f.status = com.collegeerp.erp.fee.enums.FeeStructureStatus.ACTIVE
   order by f.studentCategory, f.customCategoryName, f.id
   """)
 List<FeeStructure> findAssessmentOptions(
   @Param("collegeId") Long collegeId,
   @Param("departmentId") Long departmentId,
   @Param("academicYears") Collection<String> academicYears,
   @Param("gender") String gender,
   @Param("courseYear") String courseYear);
 default List<FeeStructure> findConfiguredAssessments(
   Long collegeId, Long departmentId, Collection<String> academicYears,
   StudentCategory category, String customCategoryName, String gender,
  String courseYear, FeeStructureStatus status) {
  if (courseYear != null) {
   List<FeeStructure> exact = customCategoryName == null
     ? findAllConfiguredAssessmentsWithoutCustomCategory(
       collegeId, departmentId, academicYears, category, gender, courseYear, status)
     : findAllConfiguredAssessmentsWithCustomCategory(
       collegeId, departmentId, academicYears, category, customCategoryName,
       gender, courseYear, status);
   if (exact.size() > 1) {
    throw new com.collegeerp.erp.common.exception.BadRequestException(
      "Multiple active fee structures match this category and gender. Deactivate the duplicate configuration.");
   }
   return exact;
  }
  Optional<FeeStructure> result;
  if (customCategoryName == null) {
   result = courseYear == null
     ? findFirstByCollegeIdAndDepartmentIdAndAcademicYearInAndStudentCategoryAndCustomCategoryNameIsNullAndGenderIgnoreCaseAndStatus(
       collegeId, departmentId, academicYears, category, gender, status)
     : Optional.empty();
  } else {
   result = courseYear == null
     ? findFirstByCollegeIdAndDepartmentIdAndAcademicYearInAndStudentCategoryAndCustomCategoryNameIgnoreCaseAndGenderIgnoreCaseAndStatus(
       collegeId, departmentId, academicYears, category, customCategoryName, gender, status)
     : Optional.empty();
  }
  return result.map(List::of).orElseGet(List::of);
 }
 boolean existsByCollegeIdAndDepartmentIdAndAcademicYearInAndStudentCategoryAndStatus(Long c,Long d,Collection<String> years,StudentCategory cat,FeeStructureStatus s);
 boolean existsByCollegeIdAndDepartmentIdAndAcademicYearInAndStudentCategoryAndCourseYearIgnoreCaseAndStatus(Long c,Long d,Collection<String> years,StudentCategory cat,String courseYear,FeeStructureStatus s);
 @Query("select distinct f from FeeStructure f where f.college.id=:collegeId and f.department.id=:departmentId and f.status=com.collegeerp.erp.fee.enums.FeeStructureStatus.ACTIVE")
 List<FeeStructure> findPublicCategoryOptions(@Param("collegeId") Long collegeId,@Param("departmentId") Long departmentId);
}
