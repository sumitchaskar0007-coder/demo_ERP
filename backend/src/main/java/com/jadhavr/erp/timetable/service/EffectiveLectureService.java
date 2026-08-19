package com.jadhavr.erp.timetable.service;

import com.jadhavr.erp.academic.entity.Subject;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.timetable.entity.LectureSubstitution;
import com.jadhavr.erp.timetable.entity.WeeklyTimetable;
import com.jadhavr.erp.timetable.entity.WeeklyTimetableEntry;
import com.jadhavr.erp.timetable.repository.LectureSubstitutionRepository;
import com.jadhavr.erp.timetable.repository.WeeklyTimetableEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EffectiveLectureService {
    private final WeeklyTimetableEntryRepository entries;
    private final LectureSubstitutionRepository substitutions;

    public EffectiveLectureService(WeeklyTimetableEntryRepository entries,
            LectureSubstitutionRepository substitutions) {
        this.entries = entries;
        this.substitutions = substitutions;
    }

    public List<EffectiveLecture> forTeacher(StaffProfile teacher, LocalDate date) {
        List<WeeklyTimetableEntry> originals = entries
                .findByTeacherIdAndTimetableStatusAndTimetableReviewStatusAndDayOfWeekOrderByPeriodStartTime(
                        teacher.getId(), WeeklyTimetable.Status.ACTIVE,
                        WeeklyTimetable.ReviewStatus.APPROVED, date.getDayOfWeek());
        Map<Long, LectureSubstitution> replacements = activeForEntries(originals, date);
        List<EffectiveLecture> result = originals.stream()
                .filter(entry -> !replacements.containsKey(entry.getId()))
                .map(entry -> original(entry, date))
                .collect(Collectors.toCollection(ArrayList::new));
        substitutions.findBySubstituteTeacherIdAndLectureDateAndStatus(
                        teacher.getId(), date, LectureSubstitution.Status.ACTIVE).stream()
                .map(this::substituted)
                .forEach(result::add);
        return result.stream()
                .sorted(Comparator.comparing(item -> item.entry().getPeriod().getStartTime()))
                .toList();
    }

    public EffectiveLecture forEntry(WeeklyTimetableEntry entry, LocalDate date) {
        return substitutions.findByTimetableEntryIdAndLectureDateAndStatus(
                        entry.getId(), date, LectureSubstitution.Status.ACTIVE)
                .map(this::substituted)
                .orElseGet(() -> original(entry, date));
    }

    public boolean isEffectiveTeacher(WeeklyTimetableEntry entry, LocalDate date, Long teacherId) {
        return forEntry(entry, date).teacher().getId().equals(teacherId);
    }

    private Map<Long, LectureSubstitution> activeForEntries(
            List<WeeklyTimetableEntry> rows, LocalDate date) {
        if (rows.isEmpty()) return Map.of();
        return substitutions.findByTimetableEntryIdInAndLectureDateAndStatus(
                        rows.stream().map(WeeklyTimetableEntry::getId).toList(), date,
                        LectureSubstitution.Status.ACTIVE).stream()
                .collect(Collectors.toMap(item -> item.getTimetableEntry().getId(), Function.identity()));
    }

    private EffectiveLecture original(WeeklyTimetableEntry entry, LocalDate date) {
        return new EffectiveLecture(entry, date, entry.getTeacher(), entry.getSubject(),
                entry.getTeacher(), null);
    }

    private EffectiveLecture substituted(LectureSubstitution substitution) {
        return new EffectiveLecture(substitution.getTimetableEntry(), substitution.getLectureDate(),
                substitution.getSubstituteTeacher(), substitution.getSubstituteSubject(),
                substitution.getOriginalTeacher(), substitution.getId());
    }

    public record EffectiveLecture(
            WeeklyTimetableEntry entry,
            LocalDate date,
            StaffProfile teacher,
            Subject subject,
            StaffProfile originalTeacher,
            Long substitutionId) {
        public boolean substituted() { return substitutionId != null; }
    }
}
