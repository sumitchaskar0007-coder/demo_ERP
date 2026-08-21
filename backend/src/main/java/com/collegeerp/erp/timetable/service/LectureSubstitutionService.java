package com.collegeerp.erp.timetable.service;

import com.collegeerp.erp.academic.entity.Subject;
import com.collegeerp.erp.academic.entity.SubjectTeacherAssignment;
import com.collegeerp.erp.academic.enums.AcademicStatus;
import com.collegeerp.erp.academic.enums.SubjectStatus;
import com.collegeerp.erp.academic.repository.SubjectTeacherAssignmentRepository;
import com.collegeerp.erp.attendance.repository.WeeklyAttendanceSessionRepository;
import com.collegeerp.erp.audit.enums.AuditAction;
import com.collegeerp.erp.audit.enums.AuditModule;
import com.collegeerp.erp.audit.service.AuditLogService;
import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.staff.entity.StaffProfile;
import com.collegeerp.erp.staff.enums.StaffStatus;
import com.collegeerp.erp.staff.enums.StaffType;
import com.collegeerp.erp.staff.repository.StaffProfileRepository;
import com.collegeerp.erp.teacher.service.TeacherNotificationService;
import com.collegeerp.erp.timetable.dto.LectureSubstitutionDtos.*;
import com.collegeerp.erp.timetable.entity.LectureSubstitution;
import com.collegeerp.erp.timetable.entity.WeeklyTimetable;
import com.collegeerp.erp.timetable.entity.WeeklyTimetableEntry;
import com.collegeerp.erp.timetable.repository.LectureSubstitutionRepository;
import com.collegeerp.erp.timetable.repository.WeeklyTimetableEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class LectureSubstitutionService {
    private static final Set<StaffType> TEACHING_TYPES = EnumSet.of(
            StaffType.HOD, StaffType.TEACHER, StaffType.CLASS_TEACHER,
            StaffType.SUBJECT_TEACHER);

    private final WeeklyTimetableEntryRepository entries;
    private final LectureSubstitutionRepository substitutions;
    private final StaffProfileRepository staff;
    private final SubjectTeacherAssignmentRepository assignments;
    private final WeeklyAttendanceSessionRepository attendanceSessions;
    private final TeacherNotificationService notifications;
    private final AuditLogService audit;
    private final Clock clock;

    @Autowired
    public LectureSubstitutionService(
            WeeklyTimetableEntryRepository entries,
            LectureSubstitutionRepository substitutions,
            StaffProfileRepository staff,
            SubjectTeacherAssignmentRepository assignments,
            WeeklyAttendanceSessionRepository attendanceSessions,
            TeacherNotificationService notifications,
            AuditLogService audit) {
        this(entries, substitutions, staff, assignments, attendanceSessions, notifications, audit,
                Clock.systemDefaultZone());
    }

    LectureSubstitutionService(
            WeeklyTimetableEntryRepository entries,
            LectureSubstitutionRepository substitutions,
            StaffProfileRepository staff,
            SubjectTeacherAssignmentRepository assignments,
            WeeklyAttendanceSessionRepository attendanceSessions,
            TeacherNotificationService notifications,
            AuditLogService audit,
            Clock clock) {
        this.entries = entries;
        this.substitutions = substitutions;
        this.staff = staff;
        this.assignments = assignments;
        this.attendanceSessions = attendanceSessions;
        this.notifications = notifications;
        this.audit = audit;
        this.clock = clock;
    }

    public DailySchedule todayForTeacher(Long teacherId) {
        StaffProfile teacher = scopedTeacher(teacherId);
        LocalDate today = LocalDate.now(clock);
        List<TodayLecture> lectures = entries
                .findByTeacherIdAndTimetableStatusAndTimetableReviewStatusAndDayOfWeekOrderByPeriodStartTime(
                        teacherId, WeeklyTimetable.Status.ACTIVE,
                        WeeklyTimetable.ReviewStatus.APPROVED, today.getDayOfWeek()).stream()
                .map(entry -> todayLecture(entry, today))
                .toList();
        return new DailySchedule(today, teacher.getId(), teacher.getFullName(), lectures);
    }

    public List<AvailableTeacher> availableTeachers(Long originalTeacherId, Long entryId) {
        StaffProfile original = scopedTeacher(originalTeacherId);
        LocalDate today = LocalDate.now(clock);
        WeeklyTimetableEntry entry = scheduledEntry(original, entryId, today, false);
        if (attendanceSessions.findByTimetableEntryIdAndAttendanceDate(entryId, today).isPresent()) {
            throw new BadRequestException("Attendance already exists for this lecture");
        }
        if (substitutions.findByTimetableEntryIdAndLectureDateAndStatus(
                entryId, today, LectureSubstitution.Status.ACTIVE).isPresent()) {
            throw new BadRequestException("This lecture has already been forwarded");
        }
        return staff.findTeachingByCollegeAndDepartment(
                        original.getCollege().getId(), entry.getTimetable().getSection().getDepartment().getId(),
                        StaffStatus.ACTIVE, TEACHING_TYPES).stream()
                .filter(candidate -> !candidate.getId().equals(originalTeacherId))
                .filter(candidate -> isFree(candidate, entry, today))
                .map(candidate -> available(candidate, entry))
                .filter(candidate -> !candidate.subjects().isEmpty())
                .toList();
    }

    @Transactional
    public SubstitutionView create(CreateSubstitutionRequest request) {
        LocalDate today = LocalDate.now(clock);
        WeeklyTimetableEntry entry = entries.findByIdForUpdate(request.timetableEntryId())
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled lecture not found"));
        requirePrincipalScope(entry);
        requireLiveToday(entry, today);
        if (attendanceSessions.findByTimetableEntryIdAndAttendanceDate(entry.getId(), today).isPresent()) {
            throw new BadRequestException("Attendance already exists for this lecture");
        }
        if (substitutions.findByTimetableEntryIdAndLectureDateAndStatus(
                entry.getId(), today, LectureSubstitution.Status.ACTIVE).isPresent()) {
            throw new BadRequestException("This lecture has already been forwarded");
        }

        StaffProfile substitute = scopedTeacher(request.substituteTeacherId());
        if (entry.getTeacher().getId().equals(substitute.getId())) {
            throw new BadRequestException("Select a different teacher for the forwarded lecture");
        }
        if (!substitute.canTeachInDepartment(entry.getTimetable().getSection().getDepartment().getId())) {
            throw new AccessDeniedException("The substitute teacher is outside this department");
        }
        if (!isFree(substitute, entry, today)) {
            throw new BadRequestException("The selected teacher is not free during this period");
        }
        Subject subject = validSubjects(substitute, entry).stream()
                .filter(item -> item.getId().equals(request.substituteSubjectId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "The selected subject is not assigned to this teacher for the current class and semester"));

        LectureSubstitution substitution = new LectureSubstitution();
        substitution.setCollege(entry.getTimetable().getCollege());
        substitution.setTimetableEntry(entry);
        substitution.setLectureDate(today);
        substitution.setOriginalTeacher(entry.getTeacher());
        substitution.setSubstituteTeacher(substitute);
        substitution.setSubstituteSubject(subject);
        substitution.setReason(request.reason().trim());
        substitution = substitutions.saveAndFlush(substitution);

        String className = className(entry);
        String time = entry.getPeriod().getStartTime() + "-" + entry.getPeriod().getEndTime();
        notifications.notifyTeacher(entry.getTeacher(), "LECTURE_FORWARDED",
                "Your " + time + " lecture for " + className + " was forwarded to "
                        + substitute.getFullName() + " for today. Reason: " + substitution.getReason());
        notifications.notifyTeacher(substitute, "SUBSTITUTE_LECTURE_ASSIGNED",
                "You are assigned " + subject.getName() + " for " + className + " today at "
                        + time + " in place of " + entry.getTeacher().getFullName());
        audit.log(AuditModule.ACADEMIC, AuditAction.ASSIGN, "LectureSubstitution",
                substitution.getId(), "Forwarded today's " + className + " lecture from "
                        + entry.getTeacher().getFullName() + " to " + substitute.getFullName());
        return view(substitution);
    }

    @Transactional
    public void cancel(Long id) {
        LectureSubstitution substitution = substitutions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture substitution not found"));
        requirePrincipalScope(substitution.getTimetableEntry());
        if (substitution.getStatus() != LectureSubstitution.Status.ACTIVE) {
            throw new BadRequestException("This lecture forwarding is already cancelled");
        }
        if (!substitution.getLectureDate().equals(LocalDate.now(clock))) {
            throw new BadRequestException("Only today's lecture forwarding can be cancelled");
        }
        if (attendanceSessions.findByTimetableEntryIdAndAttendanceDate(
                substitution.getTimetableEntry().getId(), substitution.getLectureDate()).isPresent()) {
            throw new BadRequestException("Forwarding cannot be cancelled after attendance has started");
        }
        substitution.setStatus(LectureSubstitution.Status.CANCELLED);
        substitution.setCancelledAt(LocalDateTime.now(clock));
        substitutions.save(substitution);
        notifications.notifyTeacher(substitution.getOriginalTeacher(), "LECTURE_FORWARDING_CANCELLED",
                "The forwarding of your " + className(substitution.getTimetableEntry())
                        + " lecture for today was cancelled");
        notifications.notifyTeacher(substitution.getSubstituteTeacher(), "SUBSTITUTE_LECTURE_CANCELLED",
                "Your substitute lecture for " + className(substitution.getTimetableEntry())
                        + " today was cancelled");
        audit.log(AuditModule.ACADEMIC, AuditAction.UPDATE, "LectureSubstitution", id,
                "Cancelled today's lecture forwarding");
    }

    private TodayLecture todayLecture(WeeklyTimetableEntry entry, LocalDate date) {
        SubstitutionView substitution = substitutions.findByTimetableEntryIdAndLectureDateAndStatus(
                        entry.getId(), date, LectureSubstitution.Status.ACTIVE)
                .map(this::view).orElse(null);
        return new TodayLecture(entry.getId(), entry.getPeriod().getLabel(),
                entry.getPeriod().getStartTime(), entry.getPeriod().getEndTime(),
                entry.getSubject().getId(), entry.getSubject().getCode(), entry.getSubject().getName(),
                entry.getTimetable().getSection().getDepartment().getName(),
                entry.getTimetable().getSection().getAcademicClass().getName(),
                entry.getTimetable().getSection().getName(), entry.getRoom(),
                entry.getLectureType().name(), substitution);
    }

    private AvailableTeacher available(StaffProfile teacher, WeeklyTimetableEntry entry) {
        return new AvailableTeacher(teacher.getId(), teacher.getEmployeeCode(), teacher.getFullName(),
                validSubjects(teacher, entry).stream()
                        .map(subject -> new SubjectOption(subject.getId(), subject.getCode(), subject.getName()))
                        .toList());
    }

    private List<Subject> validSubjects(StaffProfile teacher, WeeklyTimetableEntry entry) {
        Long semesterId = entry.getTimetable().getSemesterOffering() == null ? null
                : entry.getTimetable().getSemesterOffering().getCurriculumSemester().getId();
        return assignments.findByTeacherIdAndStatus(teacher.getId(), AcademicStatus.ACTIVE).stream()
                .map(SubjectTeacherAssignment::getSubject)
                .filter(subject -> subject.getStatus() == SubjectStatus.ACTIVE)
                .filter(subject -> subject.getCollege().getId().equals(entry.getTimetable().getCollege().getId()))
                .filter(subject -> subject.getAcademicClass().getId()
                        .equals(entry.getTimetable().getSection().getAcademicClass().getId()))
                .filter(subject -> Objects.equals(subject.getAcademicYear(),
                        entry.getTimetable().getSection().getAcademicYear()))
                .filter(subject -> semesterId == null || (subject.getCurriculumSemester() != null
                        && semesterId.equals(subject.getCurriculumSemester().getId())))
                .distinct()
                .sorted(Comparator.comparing(Subject::getCode))
                .toList();
    }

    private boolean isFree(StaffProfile teacher, WeeklyTimetableEntry entry, LocalDate date) {
        return entries.countApprovedTeacherConflicts(entry.getTimetable().getCollege().getId(),
                        date.getDayOfWeek(), teacher.getId(), entry.getPeriod().getStartTime(),
                        entry.getPeriod().getEndTime()) == 0
                && substitutions.countActiveConflicts(teacher.getId(), date,
                        entry.getPeriod().getStartTime(), entry.getPeriod().getEndTime()) == 0;
    }

    private WeeklyTimetableEntry scheduledEntry(
            StaffProfile original, Long entryId, LocalDate date, boolean locked) {
        WeeklyTimetableEntry entry = locked ? entries.findByIdForUpdate(entryId).orElse(null)
                : entries.findById(entryId).orElse(null);
        if (entry == null) throw new ResourceNotFoundException("Scheduled lecture not found");
        requirePrincipalScope(entry);
        requireLiveToday(entry, date);
        if (!entry.getTeacher().getId().equals(original.getId())) {
            throw new AccessDeniedException("This lecture does not belong to the selected teacher");
        }
        return entry;
    }

    private void requireLiveToday(WeeklyTimetableEntry entry, LocalDate date) {
        if (entry.getTimetable().getStatus() != WeeklyTimetable.Status.ACTIVE
                || entry.getTimetable().getReviewStatus() != WeeklyTimetable.ReviewStatus.APPROVED
                || entry.getDayOfWeek() != date.getDayOfWeek()) {
            throw new BadRequestException("Only an approved lecture scheduled for today can be forwarded");
        }
    }

    private StaffProfile scopedTeacher(Long id) {
        StaffProfile profile = staff.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teaching staff member not found"));
        Long collegeId = SecurityUtils.requireCurrentUser().getCollegeId();
        if (collegeId == null || !collegeId.equals(profile.getCollege().getId())) {
            throw new AccessDeniedException("Staff member is outside your college");
        }
        if (profile.getStatus() != StaffStatus.ACTIVE || !TEACHING_TYPES.contains(profile.getStaffType())) {
            throw new BadRequestException("Lecture forwarding is available only for active teaching staff");
        }
        return profile;
    }

    private void requirePrincipalScope(WeeklyTimetableEntry entry) {
        Long collegeId = SecurityUtils.requireCurrentUser().getCollegeId();
        if (collegeId == null || !collegeId.equals(entry.getTimetable().getCollege().getId())) {
            throw new AccessDeniedException("Lecture is outside your college");
        }
    }

    private SubstitutionView view(LectureSubstitution item) {
        return new SubstitutionView(item.getId(), item.getSubstituteTeacher().getId(),
                item.getSubstituteTeacher().getFullName(), item.getSubstituteSubject().getId(),
                item.getSubstituteSubject().getCode(), item.getSubstituteSubject().getName(),
                item.getReason());
    }

    private String className(WeeklyTimetableEntry entry) {
        return entry.getTimetable().getSection().getAcademicClass().getName() + " "
                + entry.getTimetable().getSection().getName();
    }
}
