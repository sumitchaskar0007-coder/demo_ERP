package com.jadhavr.erp.timetable.repository;
import com.jadhavr.erp.timetable.entity.WeeklyTimetableEntry; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.time.*; import java.util.*;
public interface WeeklyTimetableEntryRepository extends JpaRepository<WeeklyTimetableEntry,Long>{
 interface LectureLoadProjection {
  Long getStaffId(); String getEmployeeCode(); String getStaffName(); Long getCollegeId();
  String getCollegeName(); Long getDepartmentId(); String getDepartmentName();
  Long getWeeklyLectures(); Long getWeeklyMinutes(); Long getTheoryLectures(); Long getPracticalLectures();
 }
 List<WeeklyTimetableEntry> findByTimetableId(Long id); List<WeeklyTimetableEntry> findByPeriodId(Long id); Optional<WeeklyTimetableEntry> findByTimetableIdAndDayOfWeekAndPeriodId(Long id,DayOfWeek day,Long periodId);
 List<WeeklyTimetableEntry> findByTeacherId(Long teacherId);
 @EntityGraph(attributePaths={"teacher","timetable","timetable.section","timetable.section.department"})
 List<WeeklyTimetableEntry> findByTimetableCollegeIdAndDayOfWeekAndTimetableStatusNot(
  Long collegeId,DayOfWeek day,com.jadhavr.erp.timetable.entity.WeeklyTimetable.Status status);
 @Query(value="""
  select teacher.id "staffId",teacher.employee_code "employeeCode",teacher.full_name "staffName",
    college.id "collegeId",college.name "collegeName",department.id "departmentId",department.name "departmentName",
    count(entry.id) "weeklyLectures",
    coalesce(sum(extract(epoch from (period.end_time-period.start_time))/60),0)::bigint "weeklyMinutes",
    count(entry.id) filter(where entry.lecture_type='THEORY') "theoryLectures",
    count(entry.id) filter(where entry.lecture_type='PRACTICAL') "practicalLectures"
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
 @Query("select e from WeeklyTimetableEntry e where e.timetable.college.id=:college and e.timetable.status<>com.jadhavr.erp.timetable.entity.WeeklyTimetable.Status.ARCHIVED and (e.timetable.id=:currentTimetable or e.timetable.section.id<>:section) and e.dayOfWeek=:day and e.teacher.id=:teacher and e.period.startTime < :end and e.period.endTime > :start and (:exclude is null or e.id<>:exclude) order by e.period.startTime")
 List<WeeklyTimetableEntry> teacherConflicts(@Param("college")Long college,@Param("currentTimetable")Long currentTimetable,@Param("section")Long section,@Param("day")DayOfWeek day,@Param("teacher")Long teacher,@Param("start")LocalTime start,@Param("end")LocalTime end,@Param("exclude")Long exclude);
 @Query("select count(e) from WeeklyTimetableEntry e where e.timetable.college.id=:college and e.timetable.status<>com.jadhavr.erp.timetable.entity.WeeklyTimetable.Status.ARCHIVED and (e.timetable.id=:currentTimetable or e.timetable.section.id<>:section) and e.dayOfWeek=:day and lower(e.room)=lower(:room) and e.period.startTime < :end and e.period.endTime > :start and (:exclude is null or e.id<>:exclude)")
 long roomConflicts(@Param("college")Long college,@Param("currentTimetable")Long currentTimetable,@Param("section")Long section,@Param("day")DayOfWeek day,@Param("room")String room,@Param("start")LocalTime start,@Param("end")LocalTime end,@Param("exclude")Long exclude);
}
