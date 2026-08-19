package com.jadhavr.erp.timetable.repository;

import com.jadhavr.erp.timetable.entity.WeeklyTimetable;
import com.jadhavr.erp.academic.enums.SemesterOfferingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface WeeklyTimetableRepository extends JpaRepository<WeeklyTimetable,Long> {
    Optional<WeeklyTimetable> findFirstBySectionIdAndStatusOrderByIdDesc(
            Long sectionId, WeeklyTimetable.Status status);
    Optional<WeeklyTimetable> findFirstBySectionIdAndSemesterOfferingIdAndStatusOrderByIdDesc(
            Long sectionId, Long semesterOfferingId, WeeklyTimetable.Status status);
    Optional<WeeklyTimetable> findFirstBySectionIdAndStatusAndSemesterOfferingStatusOrderByIdDesc(
            Long sectionId, WeeklyTimetable.Status status, SemesterOfferingStatus offeringStatus);
    List<WeeklyTimetable> findBySectionDepartmentId(Long departmentId);
    List<WeeklyTimetable> findBySectionDepartmentIdAndStatus(
            Long departmentId, WeeklyTimetable.Status status);
    List<WeeklyTimetable> findBySectionDepartmentIdAndStatusNot(
            Long departmentId, WeeklyTimetable.Status status);
    List<WeeklyTimetable> findBySemesterOfferingAcademicTermIdAndStatusNot(
            Long academicTermId, WeeklyTimetable.Status status);
    List<WeeklyTimetable> findBySectionDepartmentIdAndSemesterOfferingStatusAndStatus(
            Long departmentId, SemesterOfferingStatus offeringStatus, WeeklyTimetable.Status status);
    List<WeeklyTimetable> findBySectionDepartmentIdAndSemesterOfferingStatusAndStatusNot(
            Long departmentId, SemesterOfferingStatus offeringStatus, WeeklyTimetable.Status status);
}
