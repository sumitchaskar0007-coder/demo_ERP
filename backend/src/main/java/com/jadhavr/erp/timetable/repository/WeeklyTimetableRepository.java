package com.jadhavr.erp.timetable.repository;
import com.jadhavr.erp.timetable.entity.WeeklyTimetable; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface WeeklyTimetableRepository extends JpaRepository<WeeklyTimetable,Long>{Optional<WeeklyTimetable> findBySectionId(Long sectionId);List<WeeklyTimetable> findBySectionDepartmentId(Long departmentId);}
