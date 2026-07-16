package com.jadhavr.erp.timetable.service;

import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.timetable.dto.TeacherTimetableDtos.*;
import com.jadhavr.erp.timetable.entity.WeeklyPeriod;
import com.jadhavr.erp.timetable.entity.WeeklyTimetable;
import com.jadhavr.erp.timetable.entity.WeeklyTimetableEntry;
import com.jadhavr.erp.timetable.repository.WeeklyPeriodRepository;
import com.jadhavr.erp.timetable.repository.WeeklyTimetableEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class TeacherTimetableService {
    private static final List<DayOfWeek> DAYS = List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);

    private final StaffProfileRepository staff;
    private final WeeklyTimetableEntryRepository entries;
    private final WeeklyPeriodRepository periods;
    private final Clock clock;

    @Autowired
    public TeacherTimetableService(StaffProfileRepository staff,
                                   WeeklyTimetableEntryRepository entries,
                                   WeeklyPeriodRepository periods) {
        this(staff, entries, periods, Clock.systemDefaultZone());
    }

    TeacherTimetableService(StaffProfileRepository staff,
                            WeeklyTimetableEntryRepository entries,
                            WeeklyPeriodRepository periods,
                            Clock clock) {
        this.staff = staff;
        this.entries = entries;
        this.periods = periods;
        this.clock = clock;
    }

    public TimetableResponse timetable() {
        StaffProfile teacher = currentTeacher();
        List<WeeklyTimetableEntry> teacherEntries = teacherEntries(teacher);
        DayOfWeek today = LocalDate.now(clock).getDayOfWeek();
        return new TimetableResponse(teacher.getFullName(), teacher.getEmployeeCode(),
                teacherEntries.size(), teacherEntries.stream().filter(e -> e.getDayOfWeek() == today).count(),
                today.name(), aggregatePeriods(teacherEntries), teacherEntries.stream().map(this::map).toList());
    }

    public DayResponse today() {
        return day(LocalDate.now(clock).getDayOfWeek().name());
    }

    public DayResponse day(String value) {
        StaffProfile teacher = currentTeacher();
        DayOfWeek selected = parseDay(value);
        List<WeeklyTimetableEntry> all = teacherEntries(teacher);
        return new DayResponse(selected.name(), aggregatePeriods(all), all.stream()
                .filter(e -> e.getDayOfWeek() == selected).map(this::map).toList());
    }

    public NextLectureResponse next() {
        StaffProfile teacher = currentTeacher();
        LocalDateTime now = LocalDateTime.now(clock);
        WeeklyTimetableEntry next = null;
        LocalDateTime nextAt = null;
        for (WeeklyTimetableEntry entry : teacherEntries(teacher)) {
            int offset = (entry.getDayOfWeek().getValue() - now.getDayOfWeek().getValue() + 7) % 7;
            LocalDateTime candidate = LocalDateTime.of(now.toLocalDate().plusDays(offset), entry.getPeriod().getStartTime());
            if (!candidate.isAfter(now)) candidate = candidate.plusWeeks(1);
            if (nextAt == null || candidate.isBefore(nextAt)) {
                next = entry;
                nextAt = candidate;
            }
        }
        return next == null ? new NextLectureResponse(null, null)
                : new NextLectureResponse(map(next), ChronoUnit.MINUTES.between(now, nextAt));
    }

    private StaffProfile currentTeacher() {
        return staff.findByUserId(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AccessDeniedException("Teacher profile not found"));
    }

    private List<WeeklyTimetableEntry> teacherEntries(StaffProfile teacher) {
        return entries.findByTeacherId(teacher.getId()).stream()
                .filter(entry -> entry.getTimetable().getStatus() == WeeklyTimetable.Status.ACTIVE)
                .sorted(Comparator.comparing(WeeklyTimetableEntry::getDayOfWeek)
                        .thenComparing(entry -> entry.getPeriod().getStartTime()))
                .toList();
    }

    private List<PeriodResponse> aggregatePeriods(List<WeeklyTimetableEntry> teacherEntries) {
        Map<String, PeriodResponse> rows = new TreeMap<>();
        teacherEntries.stream().map(WeeklyTimetableEntry::getTimetable).distinct().forEach(timetable ->
                periods.findByTimetableIdOrderByPosition(timetable.getId()).forEach(period ->
                        rows.putIfAbsent(key(period), period(period))));
        teacherEntries.forEach(entry -> rows.put(key(entry.getPeriod()), period(entry.getPeriod())));
        int[] position = {0};
        return rows.values().stream().map(row -> new PeriodResponse(row.key(), ++position[0], row.label(),
                row.startTime(), row.endTime(), row.kind())).toList();
    }

    private LectureResponse map(WeeklyTimetableEntry entry) {
        var section = entry.getTimetable().getSection();
        return new LectureResponse(entry.getId(), entry.getDayOfWeek().name(), key(entry.getPeriod()),
                entry.getPeriod().getLabel(), entry.getPeriod().getStartTime(), entry.getPeriod().getEndTime(),
                entry.getSubject().getId(), entry.getSubject().getName(), section.getDepartment().getName(),
                section.getAcademicClass().getName(), section.getName(), entry.getLectureType().name(),
                entry.getRemarks());
    }

    private PeriodResponse period(WeeklyPeriod period) {
        return new PeriodResponse(key(period), period.getPosition(), period.getLabel(), period.getStartTime(),
                period.getEndTime(), period.getKind().name());
    }

    private String key(WeeklyPeriod period) {
        return period.getStartTime() + "-" + period.getEndTime();
    }

    private DayOfWeek parseDay(String value) {
        try {
            DayOfWeek day = DayOfWeek.valueOf(value.toUpperCase(Locale.ROOT));
            if (!DAYS.contains(day)) throw new IllegalArgumentException();
            return day;
        } catch (RuntimeException exception) {
            throw new com.jadhavr.erp.common.exception.BadRequestException("Invalid timetable day");
        }
    }
}
