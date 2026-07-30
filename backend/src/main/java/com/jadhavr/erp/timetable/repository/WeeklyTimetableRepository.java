package com.jadhavr.erp.timetable.repository;

import com.jadhavr.erp.timetable.entity.WeeklyTimetable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface WeeklyTimetableRepository extends JpaRepository<WeeklyTimetable,Long> {
    Optional<WeeklyTimetable> findFirstBySectionIdAndStatusOrderByIdDesc(
            Long sectionId, WeeklyTimetable.Status status);
    List<WeeklyTimetable> findBySectionDepartmentId(Long departmentId);
    List<WeeklyTimetable> findBySectionDepartmentIdAndStatus(
            Long departmentId, WeeklyTimetable.Status status);
    List<WeeklyTimetable> findBySectionDepartmentIdAndStatusNot(
            Long departmentId, WeeklyTimetable.Status status);
}
