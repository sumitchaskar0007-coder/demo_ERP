package com.jadhavr.erp.timetable.repository;

import com.jadhavr.erp.timetable.entity.LectureSubstitution;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LectureSubstitutionRepository extends JpaRepository<LectureSubstitution, Long> {
    @EntityGraph(attributePaths = {
            "timetableEntry", "timetableEntry.period", "timetableEntry.subject",
            "timetableEntry.timetable", "timetableEntry.timetable.section",
            "timetableEntry.timetable.section.department", "timetableEntry.timetable.section.academicClass",
            "originalTeacher", "substituteTeacher", "substituteSubject"
    })
    Optional<LectureSubstitution> findByTimetableEntryIdAndLectureDateAndStatus(
            Long timetableEntryId, LocalDate lectureDate, LectureSubstitution.Status status);

    @EntityGraph(attributePaths = {
            "timetableEntry", "timetableEntry.period", "timetableEntry.subject",
            "timetableEntry.timetable", "timetableEntry.timetable.section",
            "timetableEntry.timetable.section.department", "timetableEntry.timetable.section.academicClass",
            "originalTeacher", "substituteTeacher", "substituteSubject"
    })
    List<LectureSubstitution> findBySubstituteTeacherIdAndLectureDateAndStatus(
            Long teacherId, LocalDate lectureDate, LectureSubstitution.Status status);

    @EntityGraph(attributePaths = {"timetableEntry", "substituteTeacher", "substituteSubject"})
    List<LectureSubstitution> findByTimetableEntryIdInAndLectureDateAndStatus(
            Collection<Long> entryIds, LocalDate lectureDate, LectureSubstitution.Status status);

    @Query("""
            select count(substitution) from LectureSubstitution substitution
            where substitution.substituteTeacher.id = :teacherId
              and substitution.lectureDate = :lectureDate
              and substitution.status = com.jadhavr.erp.timetable.entity.LectureSubstitution.Status.ACTIVE
              and substitution.timetableEntry.period.startTime < :endTime
              and substitution.timetableEntry.period.endTime > :startTime
            """)
    long countActiveConflicts(
            @Param("teacherId") Long teacherId,
            @Param("lectureDate") LocalDate lectureDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);
}
