package com.jadhavr.erp.timetable.repository;
import com.jadhavr.erp.timetable.entity.WeeklyTimetableEntry; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.time.*; import java.util.*;
public interface WeeklyTimetableEntryRepository extends JpaRepository<WeeklyTimetableEntry,Long>{
 List<WeeklyTimetableEntry> findByTimetableId(Long id); List<WeeklyTimetableEntry> findByPeriodId(Long id); Optional<WeeklyTimetableEntry> findByTimetableIdAndDayOfWeekAndPeriodId(Long id,DayOfWeek day,Long periodId);
 List<WeeklyTimetableEntry> findByTeacherId(Long teacherId);
 @Query("select count(e) from WeeklyTimetableEntry e where e.timetable.college.id=:college and e.dayOfWeek=:day and e.teacher.id=:teacher and e.period.startTime < :end and e.period.endTime > :start and (:exclude is null or e.id<>:exclude)")
 long teacherConflicts(@Param("college")Long college,@Param("day")DayOfWeek day,@Param("teacher")Long teacher,@Param("start")LocalTime start,@Param("end")LocalTime end,@Param("exclude")Long exclude);
 @Query("select count(e) from WeeklyTimetableEntry e where e.timetable.college.id=:college and e.dayOfWeek=:day and lower(e.room)=lower(:room) and e.period.startTime < :end and e.period.endTime > :start and (:exclude is null or e.id<>:exclude)")
 long roomConflicts(@Param("college")Long college,@Param("day")DayOfWeek day,@Param("room")String room,@Param("start")LocalTime start,@Param("end")LocalTime end,@Param("exclude")Long exclude);
}
