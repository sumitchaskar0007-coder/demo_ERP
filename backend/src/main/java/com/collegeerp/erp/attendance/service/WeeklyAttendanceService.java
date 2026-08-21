package com.collegeerp.erp.attendance.service;

import com.collegeerp.erp.academic.entity.*;
import com.collegeerp.erp.academic.enums.AcademicStatus;
import com.collegeerp.erp.academic.repository.StudentSectionEnrollmentRepository;
import com.collegeerp.erp.attendance.dto.WeeklyAttendanceDtos.*;
import com.collegeerp.erp.attendance.entity.*;
import com.collegeerp.erp.attendance.entity.AttendanceModels.AuditLog;
import com.collegeerp.erp.attendance.repository.*;
import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.common.exception.*;
import com.collegeerp.erp.staff.entity.StaffProfile;
import com.collegeerp.erp.staff.repository.StaffProfileRepository;
import com.collegeerp.erp.student.entity.StudentProfile;
import com.collegeerp.erp.student.repository.StudentProfileRepository;
import com.collegeerp.erp.timetable.entity.*;
import com.collegeerp.erp.timetable.repository.WeeklyTimetableEntryRepository;
import com.collegeerp.erp.timetable.service.EffectiveLectureService;
import com.collegeerp.erp.timetable.service.EffectiveLectureService.EffectiveLecture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WeeklyAttendanceService {
    private final WeeklyTimetableEntryRepository entries;
    private final WeeklyAttendanceSessionRepository sessions;
    private final WeeklyAttendanceRecordRepository records;
    private final StudentSectionEnrollmentRepository enrollments;
    private final StaffProfileRepository staff;
    private final StudentProfileRepository students;
    private final AuditLogRepository audits;
    private final EffectiveLectureService effectiveLectures;
    private final int graceMinutes;
    private final WeeklyAttendanceRecord.Status defaultStatus;

    public WeeklyAttendanceService(WeeklyTimetableEntryRepository entries,
            WeeklyAttendanceSessionRepository sessions, WeeklyAttendanceRecordRepository records,
            StudentSectionEnrollmentRepository enrollments, StaffProfileRepository staff,
            StudentProfileRepository students, AuditLogRepository audits,
            EffectiveLectureService effectiveLectures,
            @Value("${app.attendance.grace-minutes:0}") int graceMinutes,
            @Value("${app.attendance.default-status:PRESENT}") String defaultStatus) {
        this.entries = entries;
        this.sessions = sessions;
        this.records = records;
        this.enrollments = enrollments;
        this.staff = staff;
        this.students = students;
        this.audits = audits;
        this.effectiveLectures = effectiveLectures;
        this.graceMinutes = Math.max(0, graceMinutes);
        this.defaultStatus = parseStatus(defaultStatus);
    }

    public LectureResponse currentLecture() {
        StaffProfile teacher = currentStaff();
        LocalDate date = LocalDate.now();
        LocalTime now = LocalTime.now();
        return effectiveLectures.forTeacher(teacher, date).stream()
                .filter(item -> isInWindow(item.entry(), now))
                .findFirst().map(this::lecture).orElse(null);
    }

    public List<LectureResponse> todayLectures() {
        StaffProfile teacher = currentStaff();
        LocalDate today = LocalDate.now();
        return effectiveLectures.forTeacher(teacher, today).stream()
                .map(this::lecture)
                .toList();
    }

    public RosterResponse roster(Long lectureId) {
        WeeklyTimetableEntry entry = ownedActiveEntry(lectureId, true);
        LocalDate date = LocalDate.now();
        WeeklyAttendanceSession session = sessions.findByTimetableEntryIdAndAttendanceDate(lectureId, date).orElse(null);
        Map<Long, WeeklyAttendanceRecord> saved = session == null ? Map.of() : records.findBySessionIdOrderByStudentFullNameAsc(session.getId())
                .stream().collect(Collectors.toMap(r -> r.getStudent().getId(), r -> r));
        List<StudentRow> rows = activeEnrollments(entry.getTimetable().getSection()).stream().map(enrollment -> {
            StudentProfile student = enrollment.getStudent();
            WeeklyAttendanceRecord record = saved.get(student.getId());
            return new StudentRow(record == null ? null : record.getId(), student.getId(), enrollment.getRollNumber(),
                    student.getAdmissionNumber(), student.getFullName(), null,
                    record == null ? defaultStatus.name() : publicStatus(record.getStatus()),
                    record == null ? null : record.getRemarks());
        }).toList();
        boolean editable = session == null || session.getStatus() == WeeklyAttendanceSession.Status.DRAFT;
        return new RosterResponse(lecture(entry, date), rows, rows.size(), counts(rows), editable);
    }

    @Transactional
    public RosterResponse create(MarkRequest request) {
        WeeklyTimetableEntry entry = ownedActiveEntry(request.lectureId(), true);
        LocalDate today = LocalDate.now();
        WeeklyAttendanceSession session = sessions.findByTimetableEntryIdAndAttendanceDate(entry.getId(), today)
                .orElseGet(() -> createSession(entry, today));
        write(session, request.records(), request.submit());
        return roster(entry.getId());
    }

    @Transactional
    public RosterResponse update(Long sessionId, UpdateRequest request) {
        WeeklyAttendanceSession session = session(sessionId);
        requireOwner(session);
        requireApproved(session.getTimetableEntry());
        assertMarkingDay(session.getTimetableEntry());
        write(session, request.records(), request.submit());
        return roster(session.getTimetableEntry().getId());
    }

    public List<SessionSummary> teacherHistory(LocalDate from, LocalDate to, Long subjectId, Long divisionId) {
        StaffProfile teacher = currentStaff();
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusMonths(1) : from;
        validateRange(start, end);
        return sessions.findByTeacherIdAndAttendanceDateBetweenOrderByAttendanceDateDescStartTimeDesc(teacher.getId(), start, end)
                .stream().filter(s -> subjectId == null || subjectId.equals(s.getSubject().getId()))
                .filter(s -> divisionId == null || divisionId.equals(s.getSection().getId()))
                .map(this::summary).toList();
    }

    @Cacheable(cacheNames = "studentAttendanceSummary",
            key = "T(com.collegeerp.erp.auth.security.SecurityUtils).getCurrentUserId()+':' + (#year?:'all') + ':' + (#month?:'all')", sync = true)
    public StudentAttendanceResponse studentAttendance(Integer year, Integer month) {
        StudentProfile student = currentStudent();
        List<WeeklyAttendanceRecord> all = records.findByStudentIdOrderBySessionAttendanceDateDescSessionStartTimeDesc(student.getId())
                .stream().filter(r -> r.getSession().getStatus() == WeeklyAttendanceSession.Status.SUBMITTED).toList();
        if (year != null) all = all.stream().filter(r -> r.getSession().getAttendanceDate().getYear() == year).toList();
        if (month != null) all = all.stream().filter(r -> r.getSession().getAttendanceDate().getMonthValue() == month).toList();
        double percentage = percentage(all);
        return new StudentAttendanceResponse(student.getId(), student.getFullName(), student.getRollNumber(), percentage,
                indicator(percentage), subjectSummaries(all), monthSummaries(all), all.stream().map(this::studentRow).toList());
    }

    public List<SubjectSummary> studentSubjectWise() { return studentAttendance(null, null).subjects(); }
    public List<MonthSummary> studentMonthly(Integer year) {
        return studentAttendance(year == null ? LocalDate.now().getYear() : year, null).monthly();
    }

    public ReportResponse classTeacherReport(LocalDate from, LocalDate to, Long divisionId, Long subjectId) {
        StaffProfile profile = currentStaff();
        return report(from, to, subjectId, divisionId, null, null,
                profile.getId(),
                s -> s.getClassTeacher() != null && s.getClassTeacher().getId().equals(profile.getId()));
    }

    public ReportResponse hodReport(LocalDate from, LocalDate to, Long departmentId, Long divisionId,
            Long subjectId, Long teacherId) {
        StaffProfile profile = currentStaff();
        return report(from, to, subjectId, divisionId, departmentId, teacherId,
                null,
                s -> profile.canTeachInDepartment(s.getDepartment().getId()));
    }

    public ReportResponse principalReport(LocalDate from, LocalDate to, Long departmentId, Long divisionId,
            Long subjectId, Long teacherId) {
        Long college = SecurityUtils.requireCurrentUser().getCollegeId();
        return report(from, to, subjectId, divisionId, departmentId, teacherId,
                null,
                s -> SecurityUtils.isSuperAdmin() || Objects.equals(college, s.getCollege().getId()));
    }

    private ReportResponse report(LocalDate from, LocalDate to, Long subjectId, Long divisionId,
            Long departmentId, Long teacherId, Long classTeacherId, Predicate<Section> scope) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusMonths(1) : from;
        validateRange(start, end);
        Long college = SecurityUtils.requireCurrentUser().getCollegeId();
        List<WeeklyAttendanceSession> list = SecurityUtils.isSuperAdmin()
                ? sessions.findByAttendanceDateBetweenOrderByAttendanceDateDescStartTimeDesc(start, end)
                : sessions.findByCollegeIdAndAttendanceDateBetweenOrderByAttendanceDateDescStartTimeDesc(college, start, end);
        List<WeeklyAttendanceSession> scoped = list.stream().filter(s -> scope.test(s.getSection()))
                .filter(s -> subjectId == null || subjectId.equals(s.getSubject().getId()))
                .filter(s -> divisionId == null || divisionId.equals(s.getSection().getId()))
                .filter(s -> departmentId == null || departmentId.equals(s.getSection().getDepartment().getId()))
                .filter(s -> teacherId == null || teacherId.equals(s.getTeacher().getId())).toList();
        list = scoped.stream().filter(s -> s.getStatus() == WeeklyAttendanceSession.Status.SUBMITTED).toList();
        List<WeeklyAttendanceRecord> all = list.isEmpty() ? List.of()
                : records.findBySessionIdIn(list.stream().map(WeeklyAttendanceSession::getId).toList());
        Map<Long, List<WeeklyAttendanceRecord>> sessionRecords = all.stream()
                .collect(Collectors.groupingBy(r -> r.getSession().getId()));
        long present = presentCount(all), absent = absentCount(all);
        long late = 0, leave = 0;

        List<StudentSectionEnrollment> active = enrollments.findForAttendanceReport(
                        AcademicStatus.ACTIVE,
                        SecurityUtils.isSuperAdmin() ? null : college,
                        departmentId, divisionId, classTeacherId).stream()
                .filter(e -> scope.test(e.getSection()))
                .toList();
        Set<Long> allowedStudentIds = all.stream().map(r -> r.getStudent().getId()).collect(Collectors.toSet());
        if (subjectId != null || teacherId != null) active = active.stream()
                .filter(e -> allowedStudentIds.contains(e.getStudent().getId())).toList();
        Map<Long, List<WeeklyAttendanceRecord>> studentRecords = all.stream()
                .collect(Collectors.groupingBy(r -> r.getStudent().getId()));
        List<StudentAnalyticsRow> studentRows = active.stream().map(e -> studentAnalytics(e,
                studentRecords.getOrDefault(e.getStudent().getId(), List.of())))
                .sorted(Comparator.comparing(StudentAnalyticsRow::studentName, String.CASE_INSENSITIVE_ORDER)).toList();

        LocalDate today = LocalDate.now();
        Set<Long> presentToday = all.stream().filter(r -> r.getSession().getAttendanceDate().equals(today))
                .filter(this::attended).map(r -> r.getStudent().getId()).collect(Collectors.toSet());
        Set<Long> absentToday = all.stream().filter(r -> r.getSession().getAttendanceDate().equals(today))
                .filter(r -> !attended(r))
                .map(r -> r.getStudent().getId()).collect(Collectors.toSet());
        List<WeeklyTimetableEntry> expectedEntries = entries.findApprovedForAttendanceReport(
                        SecurityUtils.isSuperAdmin() ? null : college,
                        departmentId, divisionId, subjectId, teacherId, classTeacherId).stream()
                .filter(e -> scope.test(e.getTimetable().getSection()))
                .toList();
        List<WeeklyTimetableEntry> expectedToday = expectedEntries.stream()
                .filter(e -> e.getDayOfWeek() == today.getDayOfWeek()).toList();
        List<WeeklyAttendanceSession> todaySessions = (SecurityUtils.isSuperAdmin() ? sessions.findByAttendanceDateBetweenOrderByAttendanceDateDescStartTimeDesc(today, today)
                : sessions.findByCollegeIdAndAttendanceDateBetweenOrderByAttendanceDateDescStartTimeDesc(college, today, today))
                .stream().filter(s -> scope.test(s.getSection()))
                .filter(s -> subjectId == null || subjectId.equals(s.getSubject().getId()))
                .filter(s -> divisionId == null || divisionId.equals(s.getSection().getId()))
                .filter(s -> departmentId == null || departmentId.equals(s.getSection().getDepartment().getId()))
                .filter(s -> teacherId == null || teacherId.equals(s.getTeacher().getId())).toList();
        int submittedToday = (int) todaySessions.stream()
                .filter(s -> s.getStatus() == WeeklyAttendanceSession.Status.SUBMITTED).count();
        int todayLectures = expectedToday.size();
        int pendingToday = Math.max(0, todayLectures - submittedToday);
        int below75 = (int) studentRows.stream().filter(s -> s.total() > 0 && s.percentage() < 75).count();
        int below50 = (int) studentRows.stream().filter(s -> s.total() > 0 && s.percentage() < 50).count();
        List<SessionSummary> summaries = list.stream().map(s -> summary(s, sessionRecords.getOrDefault(s.getId(), List.of()))).toList();
        return new ReportResponse(start, end, list.size(), all.size(), present, absent, late, leave,
                percentage(all), studentRows.size(), presentToday.size(), absentToday.size(), todayLectures,
                submittedToday, pendingToday, (int) active.stream().map(e -> e.getSection().getDepartment().getId()).distinct().count(),
                (int) active.stream().map(e -> e.getSection().getId()).distinct().count(), below75, below50,
                trend(all, start, end, expectedEntries),
                operations(expectedToday, todaySessions, "DEPARTMENT"),
                operations(expectedToday, todaySessions, "DIVISION"), operations(expectedToday, todaySessions, "TEACHER"),
                studentRows, summaries, subjectSummaries(all), monthSummaries(all));
    }

    private List<OperationalSummary> operations(List<WeeklyTimetableEntry> expected,
            List<WeeklyAttendanceSession> actual, String level) {
        Map<Long, List<WeeklyTimetableEntry>> groups = expected.stream().collect(Collectors.groupingBy(e -> switch (level) {
            case "DEPARTMENT" -> e.getTimetable().getSection().getDepartment().getId();
            case "DIVISION" -> e.getTimetable().getSection().getId();
            default -> e.getTeacher().getId();
        }, LinkedHashMap::new, Collectors.toList()));
        return groups.entrySet().stream().map(e -> {
            WeeklyTimetableEntry first = e.getValue().get(0); Long id = e.getKey();
            String name = switch (level) {
                case "DEPARTMENT" -> first.getTimetable().getSection().getDepartment().getName();
                case "DIVISION" -> first.getTimetable().getSection().getName();
                default -> first.getTeacher().getFullName();
            };
            int submitted = (int) actual.stream().filter(s -> s.getStatus() == WeeklyAttendanceSession.Status.SUBMITTED)
                    .filter(s -> switch (level) {
                        case "DEPARTMENT" -> s.getSection().getDepartment().getId().equals(id);
                        case "DIVISION" -> s.getSection().getId().equals(id);
                        default -> s.getTeacher().getId().equals(id);
                    }).count();
            return new OperationalSummary(id, name, e.getValue().size(), submitted,
                    Math.max(0, e.getValue().size() - submitted));
        }).sorted(Comparator.comparing(OperationalSummary::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private StudentAnalyticsRow studentAnalytics(StudentSectionEnrollment enrollment, List<WeeklyAttendanceRecord> rows) {
        StudentProfile student = enrollment.getStudent(); Section section = enrollment.getSection();
        List<WeeklyAttendanceRecord> ordered = rows.stream().sorted(Comparator
                .comparing((WeeklyAttendanceRecord r) -> r.getSession().getAttendanceDate()).reversed()
                .thenComparing(r -> r.getSession().getStartTime(), Comparator.reverseOrder())).toList();
        double pct = percentage(rows);
        return new StudentAnalyticsRow(student.getId(), student.getAdmissionNumber(), enrollment.getRollNumber(),
                student.getFullName(), student.getGender(), null, student.getParentName(), student.getParentPhone(),
                section.getDepartment().getId(), section.getDepartment().getName(), enrollment.getAcademicYear(),
                section.getAcademicClass().getName(), section.getId(), section.getName(),
                section.getClassTeacher() == null ? "Unassigned" : section.getClassTeacher().getFullName(), rows.size(),
                presentCount(rows), absentCount(rows), 0, 0, pct,
                detailedIndicator(pct, rows.size()), subjectSummaries(rows), monthSummaries(rows),
                ordered.stream().map(this::studentRow).toList());
    }

    private List<TrendPoint> trend(List<WeeklyAttendanceRecord> rows, LocalDate start, LocalDate end,
            List<WeeklyTimetableEntry> expected) {
        Map<LocalDate, List<WeeklyAttendanceRecord>> byDate = rows.stream()
                .collect(Collectors.groupingBy(r -> r.getSession().getAttendanceDate()));
        LocalDate lastDate = end.isAfter(LocalDate.now()) ? LocalDate.now() : end;
        if (start.isAfter(lastDate)) return List.of();
        return start.datesUntil(lastDate.plusDays(1)).map(date -> {
                    List<WeeklyAttendanceRecord> dayRecords = byDate.getOrDefault(date, List.of());
                    int total = dayRecords.size();
                    int attended = (int) dayRecords.stream().filter(this::attended).count();
                    int lectures = (int) dayRecords.stream()
                            .map(record -> record.getSession().getId()).distinct().count();
                    int scheduledLectures = (int) expected.stream()
                            .filter(entry -> entry.getDayOfWeek() == date.getDayOfWeek()).count();
                    double averagePresent = lectures == 0 ? 0
                            : Math.round(attended * 100.0 / lectures) / 100.0;
                    double averageStudents = lectures == 0 ? 0
                            : Math.round(total * 100.0 / lectures) / 100.0;
                    return new TrendPoint(date, total, attended, percentage(dayRecords),
                            lectures, scheduledLectures, averagePresent, averageStudents);
                }).filter(point -> point.scheduledLectures() > 0 || point.lectures() > 0).toList();
    }

    private WeeklyAttendanceSession createSession(WeeklyTimetableEntry entry, LocalDate date) {
        EffectiveLecture lecture = effectiveLectures.forEntry(entry, date);
        WeeklyAttendanceSession session = new WeeklyAttendanceSession();
        session.setCollege(entry.getTimetable().getCollege());
        session.setTimetableEntry(entry);
        session.setSection(entry.getTimetable().getSection());
        session.setSubject(lecture.subject());
        session.setTeacher(lecture.teacher());
        session.setAttendanceDate(date);
        session.setStartTime(entry.getPeriod().getStartTime());
        session.setEndTime(entry.getPeriod().getEndTime());
        session.setLectureNumber(entry.getPeriod().getPosition());
        return sessions.save(session);
    }

    private void write(WeeklyAttendanceSession session, List<MarkItem> items, boolean submit) {
        if (!LocalDate.now().equals(session.getAttendanceDate()))
            throw new BadRequestException("Attendance can be entered only for today's date");
        if (session.getStatus() == WeeklyAttendanceSession.Status.SUBMITTED)
            throw new BadRequestException("Submitted attendance is locked and cannot be changed");
        Map<Long, StudentSectionEnrollment> roster = activeEnrollments(session.getSection()).stream()
                .collect(Collectors.toMap(e -> e.getStudent().getId(), e -> e));
        if (items.size() != roster.size() || items.stream().map(MarkItem::studentId).distinct().count() != roster.size())
            throw new BadRequestException("Attendance must include every active student exactly once");
        for (MarkItem item : items) {
            StudentSectionEnrollment enrollment = roster.get(item.studentId());
            if (enrollment == null) throw new AccessDeniedException("Student is outside this lecture division");
            WeeklyAttendanceRecord record = records.findBySessionIdAndStudentId(session.getId(), item.studentId()).orElseGet(() -> {
                WeeklyAttendanceRecord created = new WeeklyAttendanceRecord();
                created.setSession(session); created.setStudent(enrollment.getStudent()); return created;
            });
            record.setStatus(parseStatus(item.status()));
            record.setRemarks(clean(item.remarks()));
            records.save(record);
        }
        if (submit) {
            session.setStatus(WeeklyAttendanceSession.Status.SUBMITTED);
            session.setSubmittedAt(LocalDateTime.now());
            sessions.save(session);
        }
        audit(session, submit ? "WEEKLY_ATTENDANCE_SUBMITTED" : "WEEKLY_ATTENDANCE_DRAFT_SAVED", items.size());
    }

    private WeeklyTimetableEntry ownedActiveEntry(Long id, boolean enforceWindow) {
        WeeklyTimetableEntry entry = entries.findById(id).orElseThrow(() -> new ResourceNotFoundException("Scheduled lecture not found"));
        StaffProfile teacher = currentStaff();
        if (!effectiveLectures.isEffectiveTeacher(entry, LocalDate.now(), teacher.getId())) throw new AccessDeniedException("You can mark only a lecture assigned to you today");
        if (entry.getTimetable().getStatus() != WeeklyTimetable.Status.ACTIVE) throw new BadRequestException("The timetable is not active");
        requireApproved(entry);
        if (enforceWindow) assertMarkingDay(entry);
        return entry;
    }

    private boolean approved(WeeklyTimetableEntry entry) {
        return entry.getTimetable().getReviewStatus() == WeeklyTimetable.ReviewStatus.APPROVED;
    }

    private void requireApproved(WeeklyTimetableEntry entry) {
        if (!approved(entry))
            throw new AccessDeniedException("Attendance is unavailable until the Principal approves the timetable");
    }

    private void assertMarkingDay(WeeklyTimetableEntry entry) {
        LocalDate today = LocalDate.now();
        if (entry.getDayOfWeek() != today.getDayOfWeek())
            throw new AccessDeniedException("Attendance can be marked only on the scheduled lecture day");
    }

    private boolean isInWindow(WeeklyTimetableEntry entry, LocalTime now) {
        LocalTime start = entry.getPeriod().getStartTime();
        LocalTime end = entry.getPeriod().getEndTime().plusMinutes(graceMinutes);
        return !now.isBefore(start) && !now.isAfter(end);
    }

    private WeeklyAttendanceSession session(Long id) {
        return sessions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Attendance session not found"));
    }
    private void requireOwner(WeeklyAttendanceSession session) {
        if (!session.getTeacher().getId().equals(currentStaff().getId())) throw new AccessDeniedException("Attendance is outside your scope");
    }
    private StaffProfile currentStaff() {
        return staff.findByUserId(SecurityUtils.getCurrentUserId()).orElseThrow(() -> new AccessDeniedException("Teacher profile is required"));
    }
    private StudentProfile currentStudent() {
        return students.findByUserId(SecurityUtils.getCurrentUserId()).orElseThrow(() -> new AccessDeniedException("Student profile is required"));
    }
    private List<StudentSectionEnrollment> activeEnrollments(Section section) {
        return enrollments.findBySectionIdAndStatus(section.getId(), AcademicStatus.ACTIVE).stream()
                .sorted(Comparator.comparing(StudentSectionEnrollment::getRollNumber, Comparator.nullsLast(String::compareToIgnoreCase))).toList();
    }

    private LectureResponse lecture(WeeklyTimetableEntry e, LocalDate date) {
        return lecture(effectiveLectures.forEntry(e, date));
    }

    private LectureResponse lecture(EffectiveLecture effective) {
        WeeklyTimetableEntry e = effective.entry();
        LocalDate date = effective.date();
        WeeklyAttendanceSession session = sessions.findByTimetableEntryIdAndAttendanceDate(e.getId(), date).orElse(null);
        Section section = e.getTimetable().getSection();
        boolean active = e.getDayOfWeek() == date.getDayOfWeek() && isInWindow(e, LocalTime.now());
        boolean canMark = date.equals(LocalDate.now())
                && e.getDayOfWeek() == date.getDayOfWeek()
                && approved(e)
                && (session == null || session.getStatus() == WeeklyAttendanceSession.Status.DRAFT);
        return new LectureResponse(e.getId(), session == null ? null : session.getId(), session == null ? null : session.getStatus().name(),
                date, e.getPeriod().getLabel(), e.getPeriod().getPosition(), e.getPeriod().getStartTime(), e.getPeriod().getEndTime(),
                effective.subject().getId(), effective.subject().getName(), effective.subject().getCode(), section.getDepartment().getId(),
                section.getDepartment().getName(), section.getId(), section.getAcademicClass().getName(), section.getName(),
                e.getLectureType().name(), e.getRoom(), active, canMark);
    }

    private SessionSummary summary(WeeklyAttendanceSession s) {
        List<WeeklyAttendanceRecord> rows = records.findBySessionIdOrderByStudentFullNameAsc(s.getId());
        return summary(s, rows);
    }
    private SessionSummary summary(WeeklyAttendanceSession s, List<WeeklyAttendanceRecord> rows) {
        return new SessionSummary(s.getId(), s.getAttendanceDate(), s.getStartTime() + " - " + s.getEndTime(),
                s.getSubject().getId(), s.getSubject().getName(), s.getSection().getDepartment().getId(),
                s.getSection().getDepartment().getName(), s.getSection().getId(), s.getSection().getAcademicClass().getName(),
                s.getSection().getName(), s.getTeacher().getFullName(), s.getStatus().name(), rows.size(),
                presentCount(rows), absentCount(rows), 0, 0, percentage(rows));
    }

    private StudentHistoryRow studentRow(WeeklyAttendanceRecord r) {
        WeeklyAttendanceSession s = r.getSession();
        return new StudentHistoryRow(s.getAttendanceDate(), s.getStartTime() + " - " + s.getEndTime(),
                s.getSubject().getName(), s.getTeacher().getFullName(), publicStatus(r.getStatus()), r.getRemarks());
    }

    private List<SubjectSummary> subjectSummaries(List<WeeklyAttendanceRecord> rows) {
        return rows.stream().collect(Collectors.groupingBy(r -> r.getSession().getSubject().getId(), LinkedHashMap::new, Collectors.toList()))
                .values().stream().map(group -> {
                    WeeklyAttendanceRecord first = group.get(0); int total = group.size();
                    int attended = (int) group.stream().filter(this::attended).count();
                    int absent = (int) absentCount(group);
                    int late = 0;
                    int leave = 0;
                    double pct = total == 0 ? 0 : round(attended * 100.0 / total);
                    return new SubjectSummary(first.getSession().getSubject().getId(), first.getSession().getSubject().getName(),
                            total, attended, absent, late, leave, pct, indicator(pct));
                }).sorted(Comparator.comparing(SubjectSummary::subject)).toList();
    }

    private List<MonthSummary> monthSummaries(List<WeeklyAttendanceRecord> rows) {
        return rows.stream().collect(Collectors.groupingBy(r -> YearMonth.from(r.getSession().getAttendanceDate()), TreeMap::new, Collectors.toList()))
                .entrySet().stream().map(e -> {
                    int total = e.getValue().size(); int attended = (int) e.getValue().stream().filter(this::attended).count();
                    String label = e.getKey().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + e.getKey().getYear();
                    return new MonthSummary(label, total, attended, total == 0 ? 0 : round(attended * 100.0 / total));
                }).toList();
    }

    private boolean attended(WeeklyAttendanceRecord r) {
        return r.getStatus() == WeeklyAttendanceRecord.Status.PRESENT || r.getStatus() == WeeklyAttendanceRecord.Status.LATE;
    }
    private double percentage(List<WeeklyAttendanceRecord> rows) {
        return rows.isEmpty() ? 0 : round(rows.stream().filter(this::attended).count() * 100.0 / rows.size());
    }
    private long count(List<WeeklyAttendanceRecord> rows, WeeklyAttendanceRecord.Status status) {
        return rows.stream().filter(r -> r.getStatus() == status).count();
    }
    private long presentCount(List<WeeklyAttendanceRecord> rows) {
        return rows.stream().filter(this::attended).count();
    }
    private long absentCount(List<WeeklyAttendanceRecord> rows) {
        return rows.stream().filter(r -> !attended(r)).count();
    }
    private String publicStatus(WeeklyAttendanceRecord.Status status) {
        return status == WeeklyAttendanceRecord.Status.PRESENT || status == WeeklyAttendanceRecord.Status.LATE
                ? WeeklyAttendanceRecord.Status.PRESENT.name()
                : WeeklyAttendanceRecord.Status.ABSENT.name();
    }
    private Map<String, Long> counts(List<StudentRow> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put(WeeklyAttendanceRecord.Status.PRESENT.name(),
                rows.stream().filter(r -> WeeklyAttendanceRecord.Status.PRESENT.name().equals(r.status())).count());
        result.put(WeeklyAttendanceRecord.Status.ABSENT.name(),
                rows.stream().filter(r -> WeeklyAttendanceRecord.Status.ABSENT.name().equals(r.status())).count());
        return result;
    }
    private WeeklyAttendanceRecord.Status parseStatus(String value) {
        try {
            WeeklyAttendanceRecord.Status status =
                    WeeklyAttendanceRecord.Status.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (status != WeeklyAttendanceRecord.Status.PRESENT
                    && status != WeeklyAttendanceRecord.Status.ABSENT) {
                throw new IllegalArgumentException();
            }
            return status;
        } catch (Exception ex) {
            throw new BadRequestException("Attendance status must be PRESENT or ABSENT");
        }
    }
    private String indicator(double pct) { return pct >= 75 ? "GOOD" : pct >= 60 ? "WARNING" : "CRITICAL"; }
    private String detailedIndicator(double pct, int total) {
        if (total == 0) return "NO DATA";
        if (pct >= 95) return "EXCELLENT";
        if (pct >= 85) return "GOOD";
        if (pct >= 75) return "AVERAGE";
        if (pct >= 50) return "WARNING";
        return "CRITICAL";
    }
    private double round(double n) { return Math.round(n * 100.0) / 100.0; }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new BadRequestException("From date cannot be after to date");
        if (from.isBefore(to.minusYears(2))) throw new BadRequestException("Report range cannot exceed two years");
    }
    private void audit(WeeklyAttendanceSession session, String action, int recordCount) {
        AuditLog log = new AuditLog();
        log.setCollege(session.getCollege());
        log.setActor(session.getTeacher().getUser());
        log.setAction(action);
        log.setEntityType("WeeklyAttendanceSession");
        log.setEntityId(session.getId());
        log.setDetails("lectureId=" + session.getTimetableEntry().getId() + ",date=" + session.getAttendanceDate() + ",records=" + recordCount);
        audits.save(log);
    }
}
