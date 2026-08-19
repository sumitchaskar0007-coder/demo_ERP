package com.jadhavr.erp.timetable.service;

import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.timetable.dto.TeacherTimetableDtos.*;
import com.jadhavr.erp.timetable.entity.WeeklyPeriod;
import com.jadhavr.erp.timetable.entity.WeeklyTimetable;
import com.jadhavr.erp.timetable.entity.WeeklyTimetableEntry;
import com.jadhavr.erp.timetable.repository.WeeklyPeriodRepository;
import com.jadhavr.erp.timetable.repository.WeeklyTimetableEntryRepository;
import com.jadhavr.erp.timetable.service.EffectiveLectureService.EffectiveLecture;
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
    private final EffectiveLectureService effectiveLectures;
    private final Clock clock;

    @Autowired
    public TeacherTimetableService(StaffProfileRepository staff,
                                   WeeklyTimetableEntryRepository entries,
                                   WeeklyPeriodRepository periods,
                                   EffectiveLectureService effectiveLectures) {
        this(staff, entries, periods, effectiveLectures, Clock.systemDefaultZone());
    }

    TeacherTimetableService(StaffProfileRepository staff,
                            WeeklyTimetableEntryRepository entries,
                            WeeklyPeriodRepository periods,
                            EffectiveLectureService effectiveLectures,
                            Clock clock) {
        this.staff = staff;
        this.entries = entries;
        this.periods = periods;
        this.effectiveLectures = effectiveLectures;
        this.clock = clock;
    }

    public TimetableResponse timetable() {
        return timetable(currentTeacher());
    }

    public TimetableResponse timetableFor(Long staffId) {
        StaffProfile teacher = staff.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));
        return timetable(teacher);
    }

    private TimetableResponse timetable(StaffProfile teacher) {
        List<WeeklyTimetableEntry> teacherEntries = teacherEntries(teacher);
        LocalDate todayDate = LocalDate.now(clock);
        DayOfWeek today = todayDate.getDayOfWeek();
        return new TimetableResponse(teacher.getFullName(), teacher.getEmployeeCode(),
                teacherEntries.size(), effectiveLectures.forTeacher(teacher, todayDate).size(),
                today.name(), aggregatePeriods(teacherEntries), teacherEntries.stream().map(this::map).toList());
    }

    public DayResponse today() {
        StaffProfile teacher = currentTeacher();
        LocalDate today = LocalDate.now(clock);
        List<EffectiveLecture> lectures = effectiveLectures.forTeacher(teacher, today);
        return new DayResponse(today.getDayOfWeek().name(),
                aggregatePeriods(lectures.stream().map(EffectiveLecture::entry).toList()),
                lectures.stream().map(this::map).toList());
    }

    public DayResponse day(String value) {
        StaffProfile teacher = currentTeacher();
        DayOfWeek selected = parseDay(value);
        List<WeeklyTimetableEntry> all = teacherEntries(teacher);
        LocalDate today = LocalDate.now(clock);
        if (selected == today.getDayOfWeek()) {
            List<EffectiveLecture> lectures = effectiveLectures.forTeacher(teacher, today);
            return new DayResponse(selected.name(),
                    aggregatePeriods(lectures.stream().map(EffectiveLecture::entry).toList()),
                    lectures.stream().map(this::map).toList());
        }
        return new DayResponse(selected.name(), aggregatePeriods(all), all.stream()
                .filter(e -> e.getDayOfWeek() == selected).map(this::map).toList());
    }

    public NextLectureResponse next() {
        StaffProfile teacher = currentTeacher();
        LocalDateTime now = LocalDateTime.now(clock);
        EffectiveLecture next = null;
        LocalDateTime nextAt = null;
        List<WeeklyTimetableEntry> permanent = teacherEntries(teacher);
        for (int offset = 0; offset <= 7; offset++) {
            LocalDate date = now.toLocalDate().plusDays(offset);
            List<EffectiveLecture> candidates = offset == 0
                    ? effectiveLectures.forTeacher(teacher, date)
                    : permanent.stream().filter(entry -> entry.getDayOfWeek() == date.getDayOfWeek())
                            .map(entry -> new EffectiveLecture(entry, date, entry.getTeacher(),
                                    entry.getSubject(), entry.getTeacher(), null)).toList();
            for (EffectiveLecture lecture : candidates) {
                LocalDateTime candidate = LocalDateTime.of(date, lecture.entry().getPeriod().getStartTime());
                if (candidate.isAfter(now) && (nextAt == null || candidate.isBefore(nextAt))) {
                    next = lecture;
                    nextAt = candidate;
                }
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
                .filter(entry -> entry.getTimetable().getReviewStatus() == WeeklyTimetable.ReviewStatus.APPROVED)
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
        return map(new EffectiveLecture(entry, null, entry.getTeacher(), entry.getSubject(),
                entry.getTeacher(), null));
    }

    private LectureResponse map(EffectiveLecture lecture) {
        WeeklyTimetableEntry entry = lecture.entry();
        var section = entry.getTimetable().getSection();
        return new LectureResponse(entry.getId(), entry.getDayOfWeek().name(), key(entry.getPeriod()),
                entry.getPeriod().getLabel(), entry.getPeriod().getStartTime(), entry.getPeriod().getEndTime(),
                lecture.subject().getId(), lecture.subject().getName(), section.getDepartment().getName(),
                section.getAcademicClass().getName(), section.getName(), entry.getLectureType().name(),
                entry.getRemarks(), lecture.substituted(),
                lecture.substituted() ? lecture.originalTeacher().getFullName() : null);
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
