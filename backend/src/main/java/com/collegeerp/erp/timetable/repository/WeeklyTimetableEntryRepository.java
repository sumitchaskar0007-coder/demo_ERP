package com.collegeerp.erp.timetable.repository;
import com.collegeerp.erp.timetable.entity.WeeklyTimetableEntry; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.time.*; import java.util.*;
public interface WeeklyTimetableEntryRepository extends JpaRepository<WeeklyTimetableEntry,Long>{
 interface LectureLoadProjection {
  Long getStaffId(); String getEmployeeCode(); String getStaffName(); Long getCollegeId();
  String getCollegeName(); Long getDepartmentId(); String getDepartmentName();
  Long getWeeklyLectures(); Long getWeeklyMinutes(); Long getTheoryLectures(); Long getLabLectures(); Long getOtherLectures();
 }
 List<WeeklyTimetableEntry> findByTimetableId(Long id); List<WeeklyTimetableEntry> findByPeriodId(Long id); Optional<WeeklyTimetableEntry> findByTimetableIdAndDayOfWeekAndPeriodId(Long id,DayOfWeek day,Long periodId);
 List<WeeklyTimetableEntry> findByTeacherId(Long teacherId);
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @Query("select entry from WeeklyTimetableEntry entry where entry.id=:id")
 Optional<WeeklyTimetableEntry> findByIdForUpdate(@Param("id")Long id);
 long countByTimetableId(Long timetableId);
 long countByTeacherIdAndTimetableStatusAndTimetableReviewStatus(
  Long teacherId,com.collegeerp.erp.timetable.entity.WeeklyTimetable.Status status,
  com.collegeerp.erp.timetable.entity.WeeklyTimetable.ReviewStatus reviewStatus);
 @EntityGraph(attributePaths={"period","subject","teacher","timetable","timetable.section","timetable.section.department"})
 List<WeeklyTimetableEntry> findByTeacherIdAndTimetableStatusAndTimetableReviewStatusAndDayOfWeekOrderByPeriodStartTime(
  Long teacherId,com.collegeerp.erp.timetable.entity.WeeklyTimetable.Status status,
  com.collegeerp.erp.timetable.entity.WeeklyTimetable.ReviewStatus reviewStatus,DayOfWeek dayOfWeek);
 @EntityGraph(attributePaths={"period","subject","teacher","timetable","timetable.section","timetable.section.department","timetable.section.classTeacher"})
 @Query("""
  select entry from WeeklyTimetableEntry entry
  where entry.timetable.status=com.collegeerp.erp.timetable.entity.WeeklyTimetable.Status.ACTIVE
    and entry.timetable.reviewStatus=com.collegeerp.erp.timetable.entity.WeeklyTimetable.ReviewStatus.APPROVED
    and (:collegeId is null or entry.timetable.college.id=:collegeId)
    and (:departmentId is null or entry.timetable.section.department.id=:departmentId)
    and (:divisionId is null or entry.timetable.section.id=:divisionId)
    and (:subjectId is null or entry.subject.id=:subjectId)
    and (:teacherId is null or entry.teacher.id=:teacherId)
    and (:classTeacherId is null or entry.timetable.section.classTeacher.id=:classTeacherId)
  """)
 List<WeeklyTimetableEntry> findApprovedForAttendanceReport(
  @Param("collegeId")Long collegeId,@Param("departmentId")Long departmentId,
  @Param("divisionId")Long divisionId,@Param("subjectId")Long subjectId,
  @Param("teacherId")Long teacherId,@Param("classTeacherId")Long classTeacherId);
 @Query("""
  select count(distinct entry.timetable.id) from WeeklyTimetableEntry entry
  where entry.timetable.section.department.id=:departmentId
    and entry.timetable.status=:status
    and entry.timetable.reviewStatus=:reviewStatus
    and entry.dayOfWeek=:dayOfWeek
  """)
 long countDistinctTimetablesByDepartmentAndDay(
  @Param("departmentId")Long departmentId,
  @Param("status")com.collegeerp.erp.timetable.entity.WeeklyTimetable.Status status,
  @Param("reviewStatus")com.collegeerp.erp.timetable.entity.WeeklyTimetable.ReviewStatus reviewStatus,
  @Param("dayOfWeek")DayOfWeek dayOfWeek);
 @EntityGraph(attributePaths={"teacher","timetable","timetable.section","timetable.section.department"})
 List<WeeklyTimetableEntry> findByTimetableCollegeIdAndDayOfWeekAndTimetableStatusNot(
  Long collegeId,DayOfWeek day,com.collegeerp.erp.timetable.entity.WeeklyTimetable.Status status);
 @Query(value="""
  select teacher.id "staffId",teacher.employee_code "employeeCode",teacher.full_name "staffName",
    college.id "collegeId",college.name "collegeName",department.id "departmentId",department.name "departmentName",
    count(entry.id) "weeklyLectures",
    coalesce(sum(extract(epoch from (period.end_time-period.start_time))/60),0)::bigint "weeklyMinutes",
    count(entry.id) filter(where entry.lecture_type='THEORY') "theoryLectures",
    count(entry.id) filter(where entry.lecture_type='LAB') "labLectures",
    count(entry.id) filter(where entry.lecture_type='OTHER') "otherLectures"
  from weekly_timetable_entries entry
  join weekly_timetables timetable on timetable.id=entry.timetable_id
  join course_year_divisions section on section.id=timetable.section_id
  join colleges college on college.id=timetable.college_id
  join departments department on department.id=section.department_id
  join staff_profiles teacher on teacher.id=entry.teacher_id
  join weekly_timetable_periods period on period.id=entry.period_id
  where (:collegeId is null or college.id=:collegeId)
    and (:departmentId is null or department.id=:departmentId)
    and (:courseYearId is null or section.academic_class_id=:courseYearId)
    and (:divisionId is null or section.id=:divisionId)
    and (:staffId is null or teacher.id=:staffId)
    and timetable.status='ACTIVE'
    and timetable.review_status='APPROVED'
  group by teacher.id,teacher.employee_code,teacher.full_name,college.id,college.name,
    department.id,department.name order by teacher.full_name
  """,nativeQuery=true)
 List<LectureLoadProjection> lectureLoad(@Param("collegeId")Long collegeId,@Param("departmentId")Long departmentId,
  @Param("courseYearId")Long courseYearId,@Param("divisionId")Long divisionId,@Param("staffId")Long staffId);
 @Query("select e from WeeklyTimetableEntry e where e.timetable.college.id=:college and e.timetable.status<>com.collegeerp.erp.timetable.entity.WeeklyTimetable.Status.ARCHIVED and (e.timetable.id=:currentTimetable or e.timetable.section.id<>:section) and e.dayOfWeek=:day and e.teacher.id=:teacher and e.period.startTime < :end and e.period.endTime > :start and (:exclude is null or e.id<>:exclude) order by e.period.startTime")
 List<WeeklyTimetableEntry> teacherConflicts(@Param("college")Long college,@Param("currentTimetable")Long currentTimetable,@Param("section")Long section,@Param("day")DayOfWeek day,@Param("teacher")Long teacher,@Param("start")LocalTime start,@Param("end")LocalTime end,@Param("exclude")Long exclude);
 @Query("select count(entry) from WeeklyTimetableEntry entry where entry.timetable.college.id=:college and entry.timetable.status=com.collegeerp.erp.timetable.entity.WeeklyTimetable.Status.ACTIVE and entry.timetable.reviewStatus=com.collegeerp.erp.timetable.entity.WeeklyTimetable.ReviewStatus.APPROVED and entry.dayOfWeek=:day and entry.teacher.id=:teacher and entry.period.startTime < :end and entry.period.endTime > :start")
 long countApprovedTeacherConflicts(@Param("college")Long college,@Param("day")DayOfWeek day,@Param("teacher")Long teacher,@Param("start")LocalTime start,@Param("end")LocalTime end);
 @Query("select count(e) from WeeklyTimetableEntry e where e.timetable.college.id=:college and e.timetable.status<>com.collegeerp.erp.timetable.entity.WeeklyTimetable.Status.ARCHIVED and (e.timetable.id=:currentTimetable or e.timetable.section.id<>:section) and e.dayOfWeek=:day and lower(e.room)=lower(:room) and e.period.startTime < :end and e.period.endTime > :start and (:exclude is null or e.id<>:exclude)")
 long roomConflicts(@Param("college")Long college,@Param("currentTimetable")Long currentTimetable,@Param("section")Long section,@Param("day")DayOfWeek day,@Param("room")String room,@Param("start")LocalTime start,@Param("end")LocalTime end,@Param("exclude")Long exclude);
}
