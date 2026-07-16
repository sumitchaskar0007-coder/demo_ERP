package com.jadhavr.erp.attendance.service;

import com.jadhavr.erp.academic.entity.*;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.repository.StudentSectionEnrollmentRepository;
import com.jadhavr.erp.attendance.dto.WeeklyAttendanceDtos.*;
import com.jadhavr.erp.attendance.entity.*;
import com.jadhavr.erp.attendance.entity.AttendanceModels.AuditLog;
import com.jadhavr.erp.attendance.repository.*;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.exception.*;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import com.jadhavr.erp.timetable.entity.*;
import com.jadhavr.erp.timetable.repository.WeeklyTimetableEntryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
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
    private final int graceMinutes;
    private final WeeklyAttendanceRecord.Status defaultStatus;

    public WeeklyAttendanceService(WeeklyTimetableEntryRepository entries,
            WeeklyAttendanceSessionRepository sessions, WeeklyAttendanceRecordRepository records,
            StudentSectionEnrollmentRepository enrollments, StaffProfileRepository staff,
            StudentProfileRepository students, AuditLogRepository audits,
            @Value("${app.attendance.grace-minutes:0}") int graceMinutes,
            @Value("${app.attendance.default-status:PRESENT}") String defaultStatus) {
        this.entries = entries;
        this.sessions = sessions;
        this.records = records;
        this.enrollments = enrollments;
        this.staff = staff;
        this.students = students;
        this.audits = audits;
        this.graceMinutes = Math.max(0, graceMinutes);
        this.defaultStatus = parseStatus(defaultStatus);
    }

    public LectureResponse currentLecture() {
        StaffProfile teacher = currentStaff();
        LocalDate date = LocalDate.now();
        LocalTime now = LocalTime.now();
        return entries.findByTeacherId(teacher.getId()).stream()
                .filter(e -> e.getTimetable().getStatus() == WeeklyTimetable.Status.ACTIVE)
                .filter(e -> e.getDayOfWeek() == date.getDayOfWeek())
                .filter(e -> isInWindow(e, now))
                .sorted(Comparator.comparing(e -> e.getPeriod().getStartTime()))
                .findFirst().map(e -> lecture(e, date)).orElse(null);
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
                    record == null ? defaultStatus.name() : record.getStatus().name(),
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
        assertMarkingWindow(session.getTimetableEntry());
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
                s -> s.getSection().getClassTeacher() != null && s.getSection().getClassTeacher().getId().equals(profile.getId()));
    }

    public ReportResponse hodReport(LocalDate from, LocalDate to, Long departmentId, Long divisionId,
            Long subjectId, Long teacherId) {
        StaffProfile profile = currentStaff();
        return report(from, to, subjectId, divisionId, departmentId, teacherId,
                s -> profile.belongsToDepartment(s.getSection().getDepartment().getId()));
    }

    public ReportResponse principalReport(LocalDate from, LocalDate to, Long departmentId, Long divisionId,
            Long subjectId, Long teacherId) {
        Long college = SecurityUtils.requireCurrentUser().getCollegeId();
        return report(from, to, subjectId, divisionId, departmentId, teacherId,
                s -> SecurityUtils.isSuperAdmin() || Objects.equals(college, s.getCollege().getId()));
    }

    private ReportResponse report(LocalDate from, LocalDate to, Long subjectId, Long divisionId,
            Long departmentId, Long teacherId, Predicate<WeeklyAttendanceSession> scope) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusMonths(1) : from;
        validateRange(start, end);
        Long college = SecurityUtils.requireCurrentUser().getCollegeId();
        List<WeeklyAttendanceSession> list = SecurityUtils.isSuperAdmin()
                ? sessions.findAll().stream().filter(s -> !s.getAttendanceDate().isBefore(start) && !s.getAttendanceDate().isAfter(end)).toList()
                : sessions.findByCollegeIdAndAttendanceDateBetweenOrderByAttendanceDateDescStartTimeDesc(college, start, end);
        list = list.stream().filter(s -> s.getStatus() == WeeklyAttendanceSession.Status.SUBMITTED).filter(scope)
                .filter(s -> subjectId == null || subjectId.equals(s.getSubject().getId()))
                .filter(s -> divisionId == null || divisionId.equals(s.getSection().getId()))
                .filter(s -> departmentId == null || departmentId.equals(s.getSection().getDepartment().getId()))
                .filter(s -> teacherId == null || teacherId.equals(s.getTeacher().getId())).toList();
        List<WeeklyAttendanceRecord> all = list.stream().flatMap(s -> records.findBySessionIdOrderByStudentFullNameAsc(s.getId()).stream()).toList();
        long present = count(all, WeeklyAttendanceRecord.Status.PRESENT), absent = count(all, WeeklyAttendanceRecord.Status.ABSENT);
        long late = count(all, WeeklyAttendanceRecord.Status.LATE), leave = count(all, WeeklyAttendanceRecord.Status.LEAVE);
        return new ReportResponse(start, end, list.size(), all.size(), present, absent, late, leave,
                percentage(all), list.stream().map(this::summary).toList(), subjectSummaries(all), monthSummaries(all));
    }

    private WeeklyAttendanceSession createSession(WeeklyTimetableEntry entry, LocalDate date) {
        WeeklyAttendanceSession session = new WeeklyAttendanceSession();
        session.setCollege(entry.getTimetable().getCollege());
        session.setTimetableEntry(entry);
        session.setSection(entry.getTimetable().getSection());
        session.setSubject(entry.getSubject());
        session.setTeacher(entry.getTeacher());
        session.setAttendanceDate(date);
        session.setStartTime(entry.getPeriod().getStartTime());
        session.setEndTime(entry.getPeriod().getEndTime());
        session.setLectureNumber(entry.getPeriod().getPosition());
        return sessions.save(session);
    }

    private void write(WeeklyAttendanceSession session, List<MarkItem> items, boolean submit) {
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
        if (!entry.getTeacher().getId().equals(teacher.getId())) throw new AccessDeniedException("You can mark only your own lecture");
        if (entry.getTimetable().getStatus() != WeeklyTimetable.Status.ACTIVE) throw new BadRequestException("The timetable is not active");
        if (enforceWindow) assertMarkingWindow(entry);
        return entry;
    }

    private void assertMarkingWindow(WeeklyTimetableEntry entry) {
        LocalDate today = LocalDate.now();
        if (entry.getDayOfWeek() != today.getDayOfWeek() || !isInWindow(entry, LocalTime.now()))
            throw new AccessDeniedException("Attendance can be marked only during the scheduled lecture window");
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
        WeeklyAttendanceSession session = sessions.findByTimetableEntryIdAndAttendanceDate(e.getId(), date).orElse(null);
        Section section = e.getTimetable().getSection();
        boolean active = e.getDayOfWeek() == date.getDayOfWeek() && isInWindow(e, LocalTime.now());
        return new LectureResponse(e.getId(), session == null ? null : session.getId(), session == null ? null : session.getStatus().name(),
                date, e.getPeriod().getLabel(), e.getPeriod().getPosition(), e.getPeriod().getStartTime(), e.getPeriod().getEndTime(),
                e.getSubject().getId(), e.getSubject().getName(), e.getSubject().getCode(), section.getDepartment().getId(),
                section.getDepartment().getName(), section.getId(), section.getAcademicClass().getName(), section.getName(),
                e.getLectureType().name(), e.getRoom(), active, active && (session == null || session.getStatus() == WeeklyAttendanceSession.Status.DRAFT));
    }

    private SessionSummary summary(WeeklyAttendanceSession s) {
        List<WeeklyAttendanceRecord> rows = records.findBySessionIdOrderByStudentFullNameAsc(s.getId());
        return new SessionSummary(s.getId(), s.getAttendanceDate(), s.getStartTime() + " - " + s.getEndTime(),
                s.getSubject().getId(), s.getSubject().getName(), s.getSection().getDepartment().getId(),
                s.getSection().getDepartment().getName(), s.getSection().getId(), s.getSection().getAcademicClass().getName(),
                s.getSection().getName(), s.getTeacher().getFullName(), s.getStatus().name(), rows.size(),
                count(rows, WeeklyAttendanceRecord.Status.PRESENT), count(rows, WeeklyAttendanceRecord.Status.ABSENT),
                count(rows, WeeklyAttendanceRecord.Status.LATE), count(rows, WeeklyAttendanceRecord.Status.LEAVE), percentage(rows));
    }

    private StudentHistoryRow studentRow(WeeklyAttendanceRecord r) {
        WeeklyAttendanceSession s = r.getSession();
        return new StudentHistoryRow(s.getAttendanceDate(), s.getStartTime() + " - " + s.getEndTime(),
                s.getSubject().getName(), s.getTeacher().getFullName(), r.getStatus().name(), r.getRemarks());
    }

    private List<SubjectSummary> subjectSummaries(List<WeeklyAttendanceRecord> rows) {
        return rows.stream().collect(Collectors.groupingBy(r -> r.getSession().getSubject().getId(), LinkedHashMap::new, Collectors.toList()))
                .values().stream().map(group -> {
                    WeeklyAttendanceRecord first = group.get(0); int total = group.size();
                    int attended = (int) group.stream().filter(this::attended).count();
                    int absent = (int) count(group, WeeklyAttendanceRecord.Status.ABSENT);
                    int late = (int) count(group, WeeklyAttendanceRecord.Status.LATE);
                    int leave = (int) count(group, WeeklyAttendanceRecord.Status.LEAVE);
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
    private Map<String, Long> counts(List<StudentRow> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (WeeklyAttendanceRecord.Status s : WeeklyAttendanceRecord.Status.values()) result.put(s.name(), rows.stream().filter(r -> s.name().equals(r.status())).count());
        return result;
    }
    private WeeklyAttendanceRecord.Status parseStatus(String value) {
        try { return WeeklyAttendanceRecord.Status.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (Exception ex) { throw new BadRequestException("Attendance status must be PRESENT, ABSENT, LATE, or LEAVE"); }
    }
    private String indicator(double pct) { return pct >= 75 ? "GOOD" : pct >= 60 ? "WARNING" : "CRITICAL"; }
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
