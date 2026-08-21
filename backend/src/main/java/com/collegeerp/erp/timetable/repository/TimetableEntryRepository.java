package com.collegeerp.erp.timetable.repository;
import com.collegeerp.erp.timetable.entity.TimetableModels.TimetableEntry;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*; import java.util.*;
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry,Long>{
 List<TimetableEntry> findByTimetableIdAndCollegeIdOrderByDayOfWeekAscPeriodPeriodNumberAsc(Long timetableId,Long collegeId);
 Optional<TimetableEntry> findByIdAndCollegeId(Long id,Long collegeId);
 @Query("select e from #{#entityName} e where e.college.id=:tenant and e.dayOfWeek=:day and e.period.startTime < :end and e.period.endTime > :start and (e.teacher.id=:teacher or e.room.id=:room or (e.timetable.academicClass.id=:clazz and e.timetable.section.id=:section))")
 List<TimetableEntry> conflicts(@Param("tenant")Long tenant,@Param("day")DayOfWeek day,@Param("start")LocalTime start,@Param("end")LocalTime end,@Param("teacher")Long teacher,@Param("room")Long room,@Param("clazz")Long clazz,@Param("section")Long section);
 @Query("select e from #{#entityName} e where e.college.id=:tenant and e.timetable.section.id=:sectionId and (:day is null or e.dayOfWeek=:day)")
 List<TimetableEntry> findBySectionAndDay(@Param("tenant")Long tenant,@Param("sectionId")Long sectionId,@Param("day")DayOfWeek day);
 @Query("select e from #{#entityName} e where e.college.id=:tenant and e.teacher.id=:teacherId and e.timetable.status='PUBLISHED'")
 List<TimetableEntry> findByTeacher(@Param("tenant")Long tenant,@Param("teacherId")Long teacherId);
 @Query("select e from #{#entityName} e where e.college.id=:tenant and e.room.id=:roomId and e.timetable.status='PUBLISHED'")
 List<TimetableEntry> findByRoom(@Param("tenant")Long tenant,@Param("roomId")Long roomId);
 @Query("select e from #{#entityName} e where e.college.id=:tenant and (lower(e.subject.name) like lower(concat('%',:q,'%')) or lower(e.teacher.fullName) like lower(concat('%',:q,'%')) or lower(e.room.name) like lower(concat('%',:q,'%')))")
 List<TimetableEntry> search(@Param("tenant")Long tenant,@Param("q")String query);
}
