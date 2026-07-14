package com.jadhavr.erp.timetable.repository;
import com.jadhavr.erp.timetable.entity.TimetableModels.TimetableEntry;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*; import java.util.*;
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry,Long>{
 List<TimetableEntry> findByTimetableIdAndCollegeIdOrderByDayOfWeekAscPeriodPeriodNumberAsc(Long timetableId,Long collegeId);
 @Query("select e from TimetableEntry e where e.college.id=:tenant and e.dayOfWeek=:day and e.period.startTime < :end and e.period.endTime > :start and (e.teacher.id=:teacher or e.room.id=:room or (e.timetable.academicClass.id=:clazz and e.timetable.section.id=:section))")
 List<TimetableEntry> conflicts(@Param("tenant")Long tenant,@Param("day")DayOfWeek day,@Param("start")LocalTime start,@Param("end")LocalTime end,@Param("teacher")Long teacher,@Param("room")Long room,@Param("clazz")Long clazz,@Param("section")Long section);
}
